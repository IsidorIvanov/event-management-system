package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.KontaktDto;
import com.eventsystem.event_management_system.dto.PorukaRequest;
import com.eventsystem.event_management_system.dto.PorukaResponseDto;
import com.eventsystem.event_management_system.model.*;
import com.eventsystem.event_management_system.repository.KorisnikRepository;
import com.eventsystem.event_management_system.repository.PorukaRepository;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.repository.ZaposleniRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import com.eventsystem.event_management_system.utils.enums.UlogaZaposlenog;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PorukaService {

    private final PorukaRepository porukaRepository;
    private final KorisnikRepository korisnikRepository;
    private final RegistracijaRepository registracijaRepository;
    private final ZaposleniRepository zaposleniRepository;
    private final CurrentUserService currentUserService;
    private final SimpMessagingTemplate messagingTemplate;

    // ======================================================
    // KONTAKTI
    // ======================================================

    public List<KontaktDto> getKontakti() {
        Korisnik trenutni = currentUserService.getCurrentKorisnik();
        Long mojId = trenutni.getKorisnikId();

        List<Korisnik> kontakti;

        if (trenutni instanceof Ucesnik ucesnik) {
            kontakti = getKontaktiZaUcesnika(ucesnik);
        } else if (trenutni instanceof Zaposleni zaposleni &&
                   zaposleni.getUloga() == UlogaZaposlenog.KOORDINATOR_PROGRAMA) {
            kontakti = getKontaktiZaKoordinatora();
        } else {
            // Ostali nemaju kontakte po ulozi
            kontakti = new ArrayList<>();
        }

        // Uvek dodaj sagovornike sa kojima već postoji prepiska, čak i ako nisu u
        // listi po ulozi (npr. preporučeni korisnici koji su poslali poruku).
        Map<Long, Poruka> poslednjePoruke = getPoslednjePoruke(mojId);
        Set<Long> postojeci = kontakti.stream()
                .map(Korisnik::getKorisnikId)
                .collect(Collectors.toCollection(HashSet::new));
        for (Poruka p : poslednjePoruke.values()) {
            Korisnik drugaStrana = p.getPosiljalac().getKorisnikId().equals(mojId)
                    ? p.getPrimalac() : p.getPosiljalac();
            if (postojeci.add(drugaStrana.getKorisnikId())) {
                kontakti.add(drugaStrana);
            }
        }

        // Ukloni samog sebe ako se pojavi
        kontakti = kontakti.stream()
                .filter(k -> !k.getKorisnikId().equals(mojId))
                .distinct()
                .collect(Collectors.toList());

        return kontakti.stream()
                .map(k -> toKontaktDto(k, mojId, poslednjePoruke))
                .sorted(Comparator.comparing(
                        dto -> dto.getVremePoslednjePoruke() != null ? dto.getVremePoslednjePoruke() : java.time.LocalDateTime.MIN,
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    private List<Korisnik> getKontaktiZaUcesnika(Ucesnik ucesnik) {
        Long ucesnikId = ucesnik.getKorisnikId();

        // Nadi sve dogadjaje na koje je ucesnik registrovan
        List<Registracija> mojeRegistracije = registracijaRepository.findByUcesnikIdWithDetails(ucesnikId);
        Set<Long> mojeDogadjajIds = mojeRegistracije.stream()
                .map(r -> r.getTipKarte().getDogadjaj().getDogadjajId())
                .collect(Collectors.toSet());

        if (mojeDogadjajIds.isEmpty()) {
            // Nema registracija, kontakti su samo koordinatori programa
            return new ArrayList<>(zaposleniRepository.findByUloga(UlogaZaposlenog.KOORDINATOR_PROGRAMA));
        }

        // Nadi sve ucesnike koji su registrovani na iste dogadjaje
        Set<Korisnik> sviKontakti = new HashSet<>();

        for (Long dogadjajId : mojeDogadjajIds) {
            List<Registracija> regs = registracijaRepository.findByDogadjajIdWithDetails(dogadjajId);
            regs.stream()
                .map(Registracija::getUcesnik)
                .filter(u -> !u.getKorisnikId().equals(ucesnikId))
                .forEach(sviKontakti::add);
        }

        // Dodaj i KOORDINATOR_PROGRAMA
        sviKontakti.addAll(zaposleniRepository.findByUloga(UlogaZaposlenog.KOORDINATOR_PROGRAMA));

        return new ArrayList<>(sviKontakti);
    }

    private List<Korisnik> getKontaktiZaKoordinatora() {
        List<Korisnik> svi = new ArrayList<>();
        // Svi ucesnici
        korisnikRepository.findAll().stream()
                .filter(k -> k instanceof Ucesnik)
                .forEach(svi::add);
        // Ostali koordinatori programa
        zaposleniRepository.findByUloga(UlogaZaposlenog.KOORDINATOR_PROGRAMA).forEach(svi::add);
        return svi;
    }

    private Map<Long, Poruka> getPoslednjePoruke(Long korisnikId) {
        List<Poruka> sve = porukaRepository.findPoslednjePoruke(korisnikId);
        Map<Long, Poruka> mapa = new HashMap<>();
        for (Poruka p : sve) {
            Long drugaStrana = p.getPosiljalac().getKorisnikId().equals(korisnikId)
                    ? p.getPrimalac().getKorisnikId()
                    : p.getPosiljalac().getKorisnikId();
            mapa.putIfAbsent(drugaStrana, p);
        }
        return mapa;
    }

    private KontaktDto toKontaktDto(Korisnik k, Long mojId, Map<Long, Poruka> poslednjePoruke) {
        Poruka posle = poslednjePoruke.get(k.getKorisnikId());
        String uloga = null;
        if (k instanceof Zaposleni z) {
            uloga = z.getUloga() != null ? z.getUloga().name() : null;
        }
        long neprocitanih = porukaRepository.countUnread(mojId, k.getKorisnikId());

        return KontaktDto.builder()
                .korisnikId(k.getKorisnikId())
                .ime(k.getIme())
                .prezime(k.getPrezime())
                .tipKorisnika(k.getTipKorisnika() != null ? k.getTipKorisnika().name() : null)
                .uloga(uloga)
                .poslednjaPorukaPreview(posle != null
                        ? (posle.getSadrzaj().length() > 60
                            ? posle.getSadrzaj().substring(0, 60) + "..."
                            : posle.getSadrzaj())
                        : null)
                .vremePoslednjePoruke(posle != null ? posle.getVremeSlamja() : null)
                .neprocitanihPoruka(neprocitanih)
                .build();
    }

    // ======================================================
    // KONVERZACIJA
    // ======================================================

    @Transactional
    public List<PorukaResponseDto> getKonverzacija(Long drugiKorisnikId) {
        Korisnik trenutni = currentUserService.getCurrentKorisnik();
        Long mojId = trenutni.getKorisnikId();

        // Oznaci poruke kao procitane
        porukaRepository.markAsRead(mojId, drugiKorisnikId);

        return porukaRepository.findKonverzacija(mojId, drugiKorisnikId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ======================================================
    // SLANJE PORUKE
    // ======================================================

    @Transactional
    public PorukaResponseDto posaljiPoruku(PorukaRequest request) {
        Korisnik posiljalac = currentUserService.getCurrentKorisnik();
        Korisnik primalac = korisnikRepository.findById(request.getPrimalacId())
                .orElseThrow(() -> new RuntimeException("Primalac nije pronađen"));

        // Provjera dozvole
        validatePosalji(posiljalac, primalac);

        Poruka poruka = Poruka.builder()
                .posiljalac(posiljalac)
                .primalac(primalac)
                .sadrzaj(request.getSadrzaj())
                .procitano(false)
                .build();

        poruka = porukaRepository.save(poruka);
        PorukaResponseDto dto = toDto(poruka);

        // Pošalji putem WebSocket primalcu
        messagingTemplate.convertAndSendToUser(
                primalac.getEmail(),
                "/queue/messages",
                dto
        );

        return dto;
    }

    private void validatePosalji(Korisnik posiljalac, Korisnik primalac) {
        if (posiljalac instanceof Ucesnik ucesnik) {
            // Ucesnik moze da salje samo ucesnicima istog dogadjaja i KOORDINATOR_PROGRAMA
            if (primalac instanceof Zaposleni zaposleni) {
                if (zaposleni.getUloga() != UlogaZaposlenog.KOORDINATOR_PROGRAMA) {
                    throw new RuntimeException("Nemate dozvolu da šaljete poruke ovom korisniku.");
                }
            } else if (primalac instanceof Ucesnik priUcesnik) {
                // Provjeri da li imaju zajednicki dogadjaj
                boolean zajednickiDogadjaj = registracijaRepository
                        .findByUcesnikIdWithDetails(ucesnik.getKorisnikId())
                        .stream()
                        .map(r -> r.getTipKarte().getDogadjaj().getDogadjajId())
                        .anyMatch(dogId -> registracijaRepository
                                .existsByUcesnikKorisnikIdAndTipKarteIdDogadjajId(
                                        priUcesnik.getKorisnikId(), dogId));
                // Ako nemaju zajednički događaj, dozvoli kontakt sa učesnicima
                // objavljenih/aktivnih (preporučenih) događaja — networking pre prijave.
                boolean naAktivnomDogadjaju = !zajednickiDogadjaj && registracijaRepository
                        .findByUcesnikIdWithDetails(priUcesnik.getKorisnikId())
                        .stream()
                        .map(r -> r.getTipKarte().getDogadjaj().getStatus())
                        .anyMatch(s -> s == StatusDogadjaja.OBJAVLJEN || s == StatusDogadjaja.AKTIVAN);
                if (!zajednickiDogadjaj && !naAktivnomDogadjaju) {
                    throw new RuntimeException("Možete slati poruke samo učesnicima sa zajedničkih ili aktivnih događaja.");
                }
            } else {
                throw new RuntimeException("Nemate dozvolu da šaljete poruke ovom korisniku.");
            }
        } else if (posiljalac instanceof Zaposleni zaposleni) {
            if (zaposleni.getUloga() != UlogaZaposlenog.KOORDINATOR_PROGRAMA) {
                throw new RuntimeException("Samo Koordinator programa može slati poruke učesnicima.");
            }
            // KOORDINATOR_PROGRAMA moze da salje svim ucesnicima i drugim KOORDINATOR_PROGRAMA
            if (primalac instanceof Zaposleni priZap) {
                if (priZap.getUloga() != UlogaZaposlenog.KOORDINATOR_PROGRAMA) {
                    throw new RuntimeException("Nemate dozvolu da šaljete poruke ovom korisniku.");
                }
            }
            // else (Ucesnik) - uvek dozvoljeno
        } else {
            throw new RuntimeException("Nemate dozvolu da šaljete poruke.");
        }
    }

    // ======================================================
    // NEPROCITANE
    // ======================================================

    public long getTotalUnread() {
        Korisnik trenutni = currentUserService.getCurrentKorisnik();
        return porukaRepository.findUnreadByRecipient(trenutni.getKorisnikId()).size();
    }

    // ======================================================
    // MAPPING
    // ======================================================

    private PorukaResponseDto toDto(Poruka p) {
        return PorukaResponseDto.builder()
                .porukaId(p.getPorukaId())
                .posiljalacId(p.getPosiljalac().getKorisnikId())
                .posiljalacIme(p.getPosiljalac().getIme())
                .posiljalacPrezime(p.getPosiljalac().getPrezime())
                .primalacId(p.getPrimalac().getKorisnikId())
                .primalacIme(p.getPrimalac().getIme())
                .primalacPrezime(p.getPrimalac().getPrezime())
                .sadrzaj(p.getSadrzaj())
                .vremeSlamja(p.getVremeSlamja())
                .procitano(p.isProcitano())
                .build();
    }
}


