package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.RegistracijaDto;
import com.eventsystem.event_management_system.dto.RegistracijaResponseDto;
import com.eventsystem.event_management_system.model.*;
import com.eventsystem.event_management_system.model.compositePK.TipKarteId;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.repository.TipKarteRepository;
import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
import com.eventsystem.event_management_system.utils.enums.StatusKarte;
import com.eventsystem.event_management_system.utils.enums.StatusRegistracije;
import com.eventsystem.event_management_system.utils.enums.TipNotifikacije;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistracijaService {

    private final RegistracijaRepository registracijaRepository;
    private final TipKarteRepository tipKarteRepository;
    private final CurrentUserService currentUserService;
    private final EmailService emailService;
    private final NotifikacijaService notifikacijaService;

    @Transactional
    public RegistracijaResponseDto register(RegistracijaDto dto) {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        if (!(korisnik instanceof Ucesnik ucesnik)) {
            throw new RuntimeException("Samo učesnici mogu da se registruju na događaje.");
        }

        TipKarteId tipKarteId = new TipKarteId(dto.getDogadjajId(), dto.getNazivTipa());
        TipKarte tipKarte = tipKarteRepository.findById(tipKarteId)
                .orElseThrow(() -> new RuntimeException("Tip karte nije pronađen."));

        // Check for duplicate registration on same event (ignoring cancelled registrations)
        if (registracijaRepository.existsByUcesnikKorisnikIdAndTipKarteIdDogadjajIdAndStatusNot(
                ucesnik.getKorisnikId(), dto.getDogadjajId(), StatusRegistracije.OTKAZANA)) {
            throw new RuntimeException("Već ste registrovani na ovaj događaj.");
        }

        Dogadjaj dogadjaj = tipKarte.getDogadjaj();

        // Provera kapaciteta događaja. Ako je broj potvrđenih prijava dostigao
        // maksimalni kapacitet, učesnik se stavlja na listu čekanja (D2) umesto
        // da prijava bude odmah potvrđena.
        long potvrdjenih = registracijaRepository.countByTipKarteIdDogadjajIdAndStatus(
                dogadjaj.getDogadjajId(), StatusRegistracije.POTVRDJENA);
        boolean naCekanju = dogadjaj.getMaksKapacitet() != null
                && potvrdjenih >= dogadjaj.getMaksKapacitet();

        Registracija reg = Registracija.builder()
                .ucesnik(ucesnik)
                .tipKarte(tipKarte)
                .status(naCekanju ? StatusRegistracije.NA_CEKANJU : StatusRegistracije.POTVRDJENA)
                .statusKarte(naCekanju ? StatusKarte.NA_CEKANJU : StatusKarte.VALIDNA)
                .build();

        reg = registracijaRepository.save(reg);
        // Generate unique ticket number after save
        reg.setBrojKarte("KT-" + reg.getRegistracijaId() + "-" + dto.getDogadjajId());
        reg = registracijaRepository.save(reg);

        if (naCekanju) {
            // D2 — obavesti učesnika da je stavljen na listu čekanja (PUSH, tip DOGADJAJ).
            notifikacijaService.posalji(
                    ucesnik,
                    TipNotifikacije.DOGADJAJ,
                    KanalNotifikacije.PUSH,
                    "Događaj \"" + dogadjaj.getNaziv() + "\" je popunjen. Stavljeni ste na listu "
                            + "čekanja i obavestićemo vas ako se oslobodi mesto.",
                    dogadjaj
            );
        } else {
            // Pošalji potvrdu registracije na email (asinhrono, ne blokira odgovor)
            Lokacija lokacija = dogadjaj.getLokacija();
            String drzava = lokacija != null && lokacija.getDrzava() != null ? lokacija.getDrzava() : "";
            String grad = lokacija != null ? lokacija.getGrad() : "";
            String lokacijaText = (grad + (!drzava.isBlank() ? ", " + drzava : "")).trim();
            emailService.posaljiPotvrduRegistracije(new EmailService.PotvrdaRegistracije(
                    ucesnik.getEmail(),
                    ucesnik.getIme(),
                    ucesnik.getPrezime(),
                    reg.getBrojKarte(),
                    dogadjaj.getNaziv(),
                    String.valueOf(dogadjaj.getDatumPocetka()),
                    String.valueOf(dogadjaj.getDatumZavrsetka()),
                    lokacijaText,
                    tipKarte.getId().getNazivTipa(),
                    String.valueOf(tipKarte.getVrsta()),
                    String.valueOf(tipKarte.getCena())
            ));
        }

        return toDto(reg);
    }

    public List<RegistracijaResponseDto> getMyRegistrations() {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        return registracijaRepository
                .findByUcesnikIdWithDetails(korisnik.getKorisnikId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<RegistracijaResponseDto> getRegistracijeByDogadjaj(Long dogadjajId) {
        return registracijaRepository
                .findByDogadjajIdWithDetails(dogadjajId)
                .stream()
                .map(this::toDtoWithUcesnik)
                .collect(Collectors.toList());
    }

    @Transactional
    public RegistracijaResponseDto cancelRegistration(Long registracijaId) {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        Registracija reg = registracijaRepository.findById(registracijaId)
                .orElseThrow(() -> new RuntimeException("Registracija nije pronađena."));
        if (!reg.getUcesnik().getKorisnikId().equals(korisnik.getKorisnikId())) {
            throw new RuntimeException("Nemate pravo da otkazujete ovu registraciju.");
        }

        boolean biloPotvrdjeno = reg.getStatus() == StatusRegistracije.POTVRDJENA;
        Dogadjaj dogadjaj = reg.getTipKarte().getDogadjaj();

        reg.setStatus(StatusRegistracije.OTKAZANA);
        reg.setStatusKarte(StatusKarte.NEVAZECA);
        RegistracijaResponseDto rezultat = toDto(registracijaRepository.save(reg));

        // Otkazivanjem potvrđene prijave oslobađa se mesto — promoviši prvog sa liste čekanja (D3).
        if (biloPotvrdjeno) {
            promoviSiSaListeCekanja(dogadjaj);
        }

        return rezultat;
    }

    /**
     * Kada se oslobodi mesto na popunjenom događaju, promoviše najstariju prijavu
     * sa liste čekanja (NA_CEKANJU → POTVRDJENA) i šalje joj notifikaciju (D3).
     */
    private void promoviSiSaListeCekanja(Dogadjaj dogadjaj) {
        if (dogadjaj.getMaksKapacitet() != null) {
            long potvrdjenih = registracijaRepository.countByTipKarteIdDogadjajIdAndStatus(
                    dogadjaj.getDogadjajId(), StatusRegistracije.POTVRDJENA);
            if (potvrdjenih >= dogadjaj.getMaksKapacitet()) {
                return; // i dalje nema slobodnog mesta
            }
        }

        registracijaRepository
                .findFirstByTipKarteIdDogadjajIdAndStatusOrderByRegistracijaIdAsc(
                        dogadjaj.getDogadjajId(), StatusRegistracije.NA_CEKANJU)
                .ifPresent(prva -> {
                    prva.setStatus(StatusRegistracije.POTVRDJENA);
                    prva.setStatusKarte(StatusKarte.VALIDNA);
                    registracijaRepository.save(prva);

                    notifikacijaService.posaljiSaEmailom(
                            prva.getUcesnik(),
                            TipNotifikacije.DOGADJAJ,
                            "Oslobodilo se mesto na događaju \"" + dogadjaj.getNaziv() + "\". "
                                    + "Vaša prijava je potvrđena i karta je sada validna.",
                            dogadjaj,
                            "Oslobodilo se mesto - " + dogadjaj.getNaziv()
                    );
                });
    }

    private RegistracijaResponseDto toDto(Registracija r) {
        TipKarte tk = r.getTipKarte();
        Dogadjaj d = tk.getDogadjaj();
        Lokacija l = d.getLokacija();
        return RegistracijaResponseDto.builder()
                .registracijaId(r.getRegistracijaId())
                .dogadjajId(d.getDogadjajId())
                .dogadjajNaziv(d.getNaziv())
                .dogadjajDatumPocetka(d.getDatumPocetka().toString())
                .dogadjajDatumZavrsetka(d.getDatumZavrsetka().toString())
                .dogadjajStatus(d.getStatus() != null ? d.getStatus().name() : null)
                .lokacijaGrad(l != null ? l.getGrad() : "")
                .lokacijaDrzava(l != null ? l.getDrzava() : "")
                .nazivTipa(tk.getId().getNazivTipa())
                .vrstaKarte(tk.getVrsta())
                .datumRegistracije(r.getDatumRegistracije() != null ? r.getDatumRegistracije().toString() : null)
                .status(r.getStatus())
                .brojKarte(r.getBrojKarte())
                .statusKarte(r.getStatusKarte())
                .build();
    }

    private RegistracijaResponseDto toDtoWithUcesnik(Registracija r) {
        RegistracijaResponseDto dto = toDto(r);
        Ucesnik u = r.getUcesnik();
        dto.setUcesnikId(u.getKorisnikId());
        dto.setUcesnikIme(u.getIme());
        dto.setUcesnikPrezime(u.getPrezime());
        dto.setUcesnikEmail(u.getEmail());
        dto.setUcesnikKompanija(u.getKompanija());
        dto.setUcesnikPozicija(u.getPozicija());
        return dto;
    }
}
