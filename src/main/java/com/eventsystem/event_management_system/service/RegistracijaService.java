package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.RegistracijaDto;
import com.eventsystem.event_management_system.dto.RegistracijaResponseDto;
import com.eventsystem.event_management_system.model.*;
import com.eventsystem.event_management_system.model.compositePK.TipKarteId;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.repository.TipKarteRepository;
import com.eventsystem.event_management_system.utils.enums.StatusKarte;
import com.eventsystem.event_management_system.utils.enums.StatusRegistracije;
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

    @Transactional
    public RegistracijaResponseDto register(RegistracijaDto dto) {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        if (!(korisnik instanceof Ucesnik ucesnik)) {
            throw new RuntimeException("Samo učesnici mogu da se registruju na događaje.");
        }

        TipKarteId tipKarteId = new TipKarteId(dto.getDogadjajId(), dto.getNazivTipa());
        TipKarte tipKarte = tipKarteRepository.findById(tipKarteId)
                .orElseThrow(() -> new RuntimeException("Tip karte nije pronađen."));

        // Check for duplicate registration on same event
        if (registracijaRepository.existsByUcesnikKorisnikIdAndTipKarteIdDogadjajId(
                ucesnik.getKorisnikId(), dto.getDogadjajId())) {
            throw new RuntimeException("Već ste registrovani na ovaj događaj.");
        }

        Registracija reg = Registracija.builder()
                .ucesnik(ucesnik)
                .tipKarte(tipKarte)
                .status(StatusRegistracije.POTVRDJENA)
                .statusKarte(StatusKarte.VALIDNA)
                .build();

        reg = registracijaRepository.save(reg);
        // Generate unique ticket number after save
        reg.setBrojKarte("KT-" + reg.getRegistracijaId() + "-" + dto.getDogadjajId());
        reg = registracijaRepository.save(reg);

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

    @Transactional
    public RegistracijaResponseDto cancelRegistration(Long registracijaId) {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        Registracija reg = registracijaRepository.findById(registracijaId)
                .orElseThrow(() -> new RuntimeException("Registracija nije pronađena."));
        if (!reg.getUcesnik().getKorisnikId().equals(korisnik.getKorisnikId())) {
            throw new RuntimeException("Nemate pravo da otkazujete ovu registraciju.");
        }
        reg.setStatus(StatusRegistracije.OTKAZANA);
        reg.setStatusKarte(StatusKarte.NEVAZECA);
        return toDto(registracijaRepository.save(reg));
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
                .datumRegistracije(r.getDatumRegistracije() != null ? r.getDatumRegistracije().toString() : null)
                .status(r.getStatus())
                .brojKarte(r.getBrojKarte())
                .statusKarte(r.getStatusKarte())
                .build();
    }
}

