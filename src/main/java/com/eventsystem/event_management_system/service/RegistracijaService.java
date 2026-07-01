package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.RegistracijaDto;
import com.eventsystem.event_management_system.dto.RegistracijaResponseDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.*;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
import com.eventsystem.event_management_system.utils.enums.StatusRegistracije;
import com.eventsystem.event_management_system.utils.enums.TipNotifikacije;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Prijava i otkazivanje registracija su premesteni u proceduralni sloj baze
 * (sp_reg_prijavi, sp_reg_otkazi) preko {@link RegistracijaProcedureDao}.
 * Provera kapaciteta, lista cekanja, generisanje broja karte i atomarnost
 * su odgovornost baze; ovaj servis zadrzava autorizaciju, ucitavanje podataka
 * za odgovor i slanje notifikacija/mejlova.
 *
 * NAPOMENA: register() i cancelRegistration() NISU @Transactional — transakciju
 * u potpunosti drzi uskladistena procedura. Citanja radi DTO-a idu preko
 * JOIN FETCH upita, pa nema LazyInitialization problema van sesije.
 */
@Service
@RequiredArgsConstructor
public class RegistracijaService {

    private final RegistracijaRepository registracijaRepository;
    private final RegistracijaProcedureDao registracijaProcedureDao;
    private final CurrentUserService currentUserService;
    private final EmailService emailService;
    private final NotifikacijaService notifikacijaService;

    public RegistracijaResponseDto register(RegistracijaDto dto) {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        if (!(korisnik instanceof Ucesnik ucesnik)) {
            throw new BadRequestException("Samo učesnici mogu da se registruju na događaje.");
        }

        // Sva poslovna logika (kapacitet, lista čekanja, duplikat, broj karte) je u proceduri.
        RegistracijaProcedureDao.PrijavaRezultat rezultat =
                registracijaProcedureDao.prijavi(ucesnik.getKorisnikId(), dto.getDogadjajId(), dto.getNazivTipa());

        Registracija reg = registracijaRepository.findByIdWithDetails(rezultat.registracijaId())
                .orElseThrow(() -> new NotFoundException("Registracija nije pronađena nakon kreiranja."));

        Dogadjaj dogadjaj = reg.getTipKarte().getDogadjaj();

        if (StatusRegistracije.NA_CEKANJU.name().equals(rezultat.status())) {
            // D2 — obavesti učesnika da je na listi čekanja (PUSH, tip DOGADJAJ).
            notifikacijaService.posalji(
                    ucesnik,
                    TipNotifikacije.DOGADJAJ,
                    KanalNotifikacije.PUSH,
                    "Događaj \"" + dogadjaj.getNaziv() + "\" je popunjen. Stavljeni ste na listu "
                            + "čekanja i obavestićemo vas ako se oslobodi mesto.",
                    dogadjaj
            );
        } else {
            // Potvrda registracije na email.
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
                    reg.getTipKarte().getId().getNazivTipa(),
                    String.valueOf(reg.getTipKarte().getVrsta()),
                    String.valueOf(reg.getTipKarte().getCena())
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

    public RegistracijaResponseDto cancelRegistration(Long registracijaId) {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();

        // Podatke za D4 notifikaciju učitavamo pre poziva procedure (naziv događaja, broj karte).
        Registracija pre = registracijaRepository.findByIdWithDetails(registracijaId)
                .orElseThrow(() -> new NotFoundException("Registracija nije pronađena."));
        Dogadjaj dogadjaj = pre.getTipKarte().getDogadjaj();
        String brojKarte = pre.getBrojKarte();

        // Otkazivanje + FIFO promocija su u proceduri (atomarno). Vraća ID promovisanog ili null.
        Long promovisanId = registracijaProcedureDao.otkazi(registracijaId, korisnik.getKorisnikId());

        // Ponovo učitaj otkazanu registraciju sa ažuriranim statusom za odgovor.
        Registracija otkazana = registracijaRepository.findByIdWithDetails(registracijaId)
                .orElseThrow(() -> new NotFoundException("Registracija nije pronađena."));

        // D4 — potvrda otkaza (samo email).
        notifikacijaService.posaljiEmail(
                otkazana.getUcesnik(),
                TipNotifikacije.DOGADJAJ,
                "Vaša prijava za događaj \"" + dogadjaj.getNaziv() + "\" je otkazana, a karta "
                        + (brojKarte != null ? "\"" + brojKarte + "\" " : "")
                        + "je poništena.",
                dogadjaj,
                "Otkazana prijava - " + dogadjaj.getNaziv()
        );

        // D3 — ako je neko promovisan sa liste čekanja, obavesti ga.
        if (promovisanId != null) {
            registracijaRepository.findByIdWithDetails(promovisanId).ifPresent(prom ->
                    notifikacijaService.posaljiSaEmailom(
                            prom.getUcesnik(),
                            TipNotifikacije.DOGADJAJ,
                            "Oslobodilo se mesto na događaju \"" + dogadjaj.getNaziv() + "\". "
                                    + "Vaša prijava je potvrđena i karta je sada validna.",
                            dogadjaj,
                            "Oslobodilo se mesto - " + dogadjaj.getNaziv()
                    )
            );
        }

        return toDto(otkazana);
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
