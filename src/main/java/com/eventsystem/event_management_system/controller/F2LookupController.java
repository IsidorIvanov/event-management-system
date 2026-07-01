package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.KlijentDto;
import com.eventsystem.event_management_system.model.Klijent;
import com.eventsystem.event_management_system.repository.KlijentRepository;
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

    @GetMapping("/klijenti")
    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    public List<KlijentDto> getKlijenti() {
        return klijentRepository.findAll().stream()
                .map(this::toKlijentDto)
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

}
