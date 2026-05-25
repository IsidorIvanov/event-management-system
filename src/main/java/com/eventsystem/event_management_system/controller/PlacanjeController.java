package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.PlacanjeDto;
import com.eventsystem.event_management_system.model.Placanje;
import com.eventsystem.event_management_system.service.PlacanjeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/placanja")
@RequiredArgsConstructor
public class PlacanjeController {

    private final PlacanjeService placanjeService;

    @PostMapping("/faktura/{fakturaId}")
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<Placanje> create(@PathVariable Long fakturaId, @RequestBody PlacanjeDto dto) {
        return ResponseEntity.ok(placanjeService.create(fakturaId, dto));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<Placanje> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(placanjeService.confirm(id));
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<Placanje> fail(@PathVariable Long id) {
        return ResponseEntity.ok(placanjeService.fail(id));
    }
}
