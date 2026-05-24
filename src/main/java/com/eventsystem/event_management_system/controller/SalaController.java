package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.SalaDto;
import com.eventsystem.event_management_system.model.Sala;
import com.eventsystem.event_management_system.service.SalaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sala")
@RequiredArgsConstructor
public class SalaController {

    private final SalaService salaService;

    @PostMapping("")
    public ResponseEntity<SalaDto> createSala(@Valid @RequestBody SalaDto salaDto) {
        return ResponseEntity.ok(salaService.createSala(salaDto));
    }

    @PutMapping("/{lokacijaId}/{nazivSale}")
    public ResponseEntity<SalaDto> updateSala(
            @PathVariable Long lokacijaId,
            @PathVariable String nazivSale,
            @Valid @RequestBody SalaDto salaDto) {
        return ResponseEntity.ok(salaService.updateSala(lokacijaId, nazivSale, salaDto));
    }

    @DeleteMapping("/{lokacijaId}/{nazivSale}")
    public ResponseEntity<String> deleteSala(
            @PathVariable Long lokacijaId,
            @PathVariable String nazivSale) {
        salaService.deleteSala(lokacijaId, nazivSale);
        return ResponseEntity.ok("Sala '" + nazivSale + "' na lokaciji " + lokacijaId + " uspesno obrisana.");
    }
}
