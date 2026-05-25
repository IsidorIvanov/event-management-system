package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.KlijentDto;
import com.eventsystem.event_management_system.dto.UgovorDto;
import com.eventsystem.event_management_system.model.Klijent;
import com.eventsystem.event_management_system.model.Ugovor;
import com.eventsystem.event_management_system.repository.KlijentRepository;
import com.eventsystem.event_management_system.repository.UgovorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class F2LookupController {

    private final KlijentRepository klijentRepository;
    private final UgovorRepository ugovorRepository;

    @GetMapping("/klijenti")
    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    public List<KlijentDto> getKlijenti() {
        return klijentRepository.findAll().stream()
                .map(this::toKlijentDto)
                .toList();
    }

    @GetMapping("/ugovori")
    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    public List<UgovorDto> getUgovori(@RequestParam Long dobavljacId) {
        return ugovorRepository.findByDobavljacDobavljacId(dobavljacId).stream()
                .map(this::toUgovorDto)
                .toList();
    }

    private KlijentDto toKlijentDto(Klijent klijent) {
        return KlijentDto.builder()
                .klijentId(klijent.getKorisnikId())
                .korisnikId(klijent.getKorisnikId())
                .ime(klijent.getIme())
                .prezime(klijent.getPrezime())
                .email(klijent.getEmail())
                .nazivFirme(klijent.getNazivFirme())
                .pib(klijent.getPib())
                .kontaktOsoba(klijent.getKontaktOsoba())
                .build();
    }

    private UgovorDto toUgovorDto(Ugovor ugovor) {
        return UgovorDto.builder()
                .ugovorId(ugovor.getUgovorId())
                .dobavljacId(ugovor.getDobavljac().getDobavljacId())
                .nabavkaId(ugovor.getNabavka() != null ? ugovor.getNabavka().getNabavkaId() : null)
                .dokumentUrl(ugovor.getDokumentUrl())
                .status(ugovor.getStatus())
                .kreiranAt(ugovor.getKreiranAt())
                .vaziDo(ugovor.getVaziDo())
                .build();
    }
}
