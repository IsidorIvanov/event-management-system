package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.NotifikacijaResponseDto;
import com.eventsystem.event_management_system.event.NotifikacijaCreatedEvent;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Korisnik;
import com.eventsystem.event_management_system.model.Notifikacija;
import com.eventsystem.event_management_system.model.Ucesnik;
import com.eventsystem.event_management_system.repository.NotifikacijaRepository;
import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
import com.eventsystem.event_management_system.utils.enums.StatusNotifikacije;
import com.eventsystem.event_management_system.utils.enums.TipNotifikacije;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotifikacijaService {

    private final NotifikacijaRepository notifikacijaRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Kreira i sačuvava notifikaciju za učesnika, pa objavljuje
     * {@link NotifikacijaCreatedEvent}. Poziva se unutar transakcije poslovne
     * operacije — upis je atoman sa njom, a isporuka (PUSH/EMAIL) se dešava tek
     * nakon commit-a (vidi {@code NotifikacijaEventListener}).
     */
    @Transactional
    public Notifikacija posalji(Ucesnik primalac, TipNotifikacije tip, KanalNotifikacije kanal,
                                String sadrzaj, Dogadjaj dogadjaj) {
        return posalji(primalac, tip, kanal, sadrzaj, dogadjaj, null);
    }

    /**
     * Kao {@link #posalji}, ali notifikaciju (PUSH u aplikaciji) dodatno
     * isporučuje i mejlom sa zadatim naslovom. Koristi se za važna obaveštenja
     * koja korisnik treba da dobije i van aplikacije (npr. D3 — oslobođeno mesto).
     */
    @Transactional
    public Notifikacija posaljiSaEmailom(Ucesnik primalac, TipNotifikacije tip, String sadrzaj,
                                         Dogadjaj dogadjaj, String emailNaslov) {
        return posalji(primalac, tip, KanalNotifikacije.PUSH, sadrzaj, dogadjaj, emailNaslov);
    }

    @Transactional
    public Notifikacija posalji(Ucesnik primalac, TipNotifikacije tip, KanalNotifikacije kanal,
                                String sadrzaj, Dogadjaj dogadjaj, String emailNaslov) {
        Notifikacija notifikacija = Notifikacija.builder()
                .korisnik(primalac)
                .dogadjaj(dogadjaj)
                .tip(tip)
                .kanal(kanal)
                .sadrzaj(sadrzaj)
                .status(StatusNotifikacije.NEPROCITANO)
                .build();

        notifikacija = notifikacijaRepository.save(notifikacija);
        eventPublisher.publishEvent(new NotifikacijaCreatedEvent(
                primalac.getEmail(), primalac.getIme(), kanal, emailNaslov, toDto(notifikacija)));
        return notifikacija;
    }

    @Transactional(readOnly = true)
    public List<NotifikacijaResponseDto> getMojeNotifikacije() {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        return notifikacijaRepository.findMojeWithDogadjaj(korisnik.getKorisnikId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countNeprocitane() {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        return notifikacijaRepository.countByKorisnikKorisnikIdAndStatus(
                korisnik.getKorisnikId(), StatusNotifikacije.NEPROCITANO);
    }

    @Transactional
    public NotifikacijaResponseDto oznaciKaoProcitano(Long notifikacijaId) {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        Notifikacija notifikacija = notifikacijaRepository.findById(notifikacijaId)
                .orElseThrow(() -> new RuntimeException("Notifikacija nije pronađena."));
        if (!notifikacija.getKorisnik().getKorisnikId().equals(korisnik.getKorisnikId())) {
            throw new RuntimeException("Nemate pravo na ovu notifikaciju.");
        }
        notifikacija.setStatus(StatusNotifikacije.PROCITANO);
        return toDto(notifikacijaRepository.save(notifikacija));
    }

    private NotifikacijaResponseDto toDto(Notifikacija n) {
        Dogadjaj d = n.getDogadjaj();
        return NotifikacijaResponseDto.builder()
                .notifikacijaId(n.getNotifikacijaId())
                .tip(n.getTip())
                .kanal(n.getKanal())
                .sadrzaj(n.getSadrzaj())
                .vremeSlanja(n.getVremeSlanja())
                .status(n.getStatus())
                .dogadjajId(d != null ? d.getDogadjajId() : null)
                .dogadjajNaziv(d != null ? d.getNaziv() : null)
                .build();
    }
}
