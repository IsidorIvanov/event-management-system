package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.FinansijskiIzvestajDto;
import com.eventsystem.event_management_system.dto.GenerateIzvestajRequest;
import com.eventsystem.event_management_system.service.FinansijskiIzvestajService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/izvestaji")
@RequiredArgsConstructor
public class FinansijskiIzvestajController {

    private final FinansijskiIzvestajService finansijskiIzvestajService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public FinansijskiIzvestajDto generate(@Valid @RequestBody GenerateIzvestajRequest request) {
        return finansijskiIzvestajService.generate(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    public List<FinansijskiIzvestajDto> list() {
        return finansijskiIzvestajService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    public FinansijskiIzvestajDto getById(@PathVariable Long id) {
        return finansijskiIzvestajService.getById(id);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return finansijskiIzvestajService.download(id);
    }
}
