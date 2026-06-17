package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.PrimeniCenuRequestDto;
import com.eventsystem.event_management_system.dto.PredlogCeneKarteDto;
import com.eventsystem.event_management_system.dto.TipKarteDto;
import com.eventsystem.event_management_system.service.DinamickoCeneService;
import com.eventsystem.event_management_system.service.TipKarteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tip-karte")
@RequiredArgsConstructor
public class TipKarteController {

    private final TipKarteService tipKarteService;
    private final DinamickoCeneService dinamickoCeneService;

    @PostMapping("")
    public ResponseEntity<TipKarteDto> createTipKarte(@Valid @RequestBody TipKarteDto tipKarteDto) {
        return ResponseEntity.ok(tipKarteService.createTipKarte(tipKarteDto));
    }

    @PutMapping("/{dogadjajId}/{nazivTipa}")
    public ResponseEntity<TipKarteDto> updateTipKarte(
            @PathVariable Long dogadjajId,
            @PathVariable String nazivTipa,
            @Valid @RequestBody TipKarteDto tipKarteDto) {
        return ResponseEntity.ok(tipKarteService.updateTipKarte(dogadjajId, nazivTipa, tipKarteDto));
    }

    @DeleteMapping("/{dogadjajId}/{nazivTipa}")
    public ResponseEntity<String> deleteTipKarte(
            @PathVariable Long dogadjajId,
            @PathVariable String nazivTipa) {
        tipKarteService.deleteTipKarte(dogadjajId, nazivTipa);
        return ResponseEntity.ok("Tip karte '" + nazivTipa + "' za događaj " + dogadjajId + " uspešno obrisan.");
    }

    @PatchMapping("/{dogadjajId}/{nazivTipa}/cena")
    public ResponseEntity<PredlogCeneKarteDto> primeniCenu(
            @PathVariable Long dogadjajId,
            @PathVariable String nazivTipa,
            @Valid @RequestBody PrimeniCenuRequestDto request) {
        return ResponseEntity.ok(dinamickoCeneService.primeniCenuKarte(
                dogadjajId, nazivTipa, request.getCena()));
    }

    @GetMapping("/dogadjaj/{dogadjajId}")
    public ResponseEntity<List<TipKarteDto>> getTipKarteByDogadjaj(@PathVariable Long dogadjajId) {
        return ResponseEntity.ok(tipKarteService.getTipKarteByDogadjaj(dogadjajId));
    }
}
