package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.FakturaDto;
import com.eventsystem.event_management_system.dto.StavkaFaktureDto;
import com.eventsystem.event_management_system.model.StavkaFakture;
import com.eventsystem.event_management_system.service.FakturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fakture")
@RequiredArgsConstructor
public class FakturaController {

    private final FakturaService fakturaService;

    @GetMapping
    public ResponseEntity<List<FakturaDto>> all() {
        return ResponseEntity.ok(fakturaService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FakturaDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(fakturaService.toDto(fakturaService.findEntity(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<FakturaDto> create(@RequestBody FakturaDto dto) {
        return ResponseEntity.ok(fakturaService.create(dto));
    }

    @PostMapping("/{id}/stavke")
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<StavkaFakture> addStavka(@PathVariable Long id, @RequestBody StavkaFaktureDto dto) {
        return ResponseEntity.ok(fakturaService.addStavka(id, dto));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        fakturaService.cancelFaktura(id);
        return ResponseEntity.ok().build();
    }
}
