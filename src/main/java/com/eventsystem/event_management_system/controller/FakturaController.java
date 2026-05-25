package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.FakturaDto;
import com.eventsystem.event_management_system.dto.StavkaFaktureDto;
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
        return ResponseEntity.ok(fakturaService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<FakturaDto> create(@RequestBody FakturaDto dto) {
        return ResponseEntity.ok(fakturaService.create(dto));
    }

    @PostMapping("/{id}/stavke")
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<StavkaFaktureDto> addStavka(@PathVariable Long id, @RequestBody StavkaFaktureDto dto) {
        return ResponseEntity.ok(fakturaService.addStavka(id, dto));
    }

    @DeleteMapping("/{fakturaId}/stavke/{stavkaId}")
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<Void> deleteStavka(@PathVariable Long fakturaId, @PathVariable Long stavkaId) {
        fakturaService.deleteStavka(fakturaId, stavkaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<FakturaDto> issue(@PathVariable Long id) {
        return ResponseEntity.ok(fakturaService.issue(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        fakturaService.cancelFaktura(id);
        return ResponseEntity.ok().build();
    }
}
