package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.SesijaDetaljDto;
import com.eventsystem.event_management_system.dto.SesijaDto;
import com.eventsystem.event_management_system.service.SesijaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sesija")
@RequiredArgsConstructor
public class SesijaController {

    private final SesijaService sesijaService;

    @PostMapping("")
    public ResponseEntity<SesijaDto> saveSesija(@RequestBody SesijaDto sesijaDto) {
        return ResponseEntity.ok(sesijaService.createSesija(sesijaDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SesijaDto> updateSesija(@PathVariable Long id, @RequestBody SesijaDto sesijaDto) {
        return ResponseEntity.ok(sesijaService.updateSesija(id, sesijaDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSesija(@PathVariable Long id) {
        sesijaService.deleteSesija(id);
        return ResponseEntity.ok("Sesija with id " + id + " deleted successfully.");
    }

    @GetMapping("/dogadjaj/{dogadjajId}")
    public ResponseEntity<List<SesijaDto>> getSesijeByDogadjaj(@PathVariable Long dogadjajId) {
        return ResponseEntity.ok(sesijaService.getSesijeByDogadjaj(dogadjajId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SesijaDto> getSesijaById(@PathVariable Long id) {
        return ResponseEntity.ok(sesijaService.getSesijaById(id));
    }

    @GetMapping("/{id}/detalj")
    public ResponseEntity<SesijaDetaljDto> getSesijaDetalj(@PathVariable Long id) {
        return ResponseEntity.ok(sesijaService.getSesijaDetalj(id));
    }

    @PostMapping("/{sesijaId}/govornici/{govornikId}")
    public ResponseEntity<SesijaDto> addGovornikToSesija(
            @PathVariable Long sesijaId,
            @PathVariable Long govornikId) {
        return ResponseEntity.ok(sesijaService.addGovornikToSesija(sesijaId, govornikId));
    }

    @DeleteMapping("/{sesijaId}/govornici/{govornikId}")
    public ResponseEntity<SesijaDto> removeGovornikFromSesija(
            @PathVariable Long sesijaId,
            @PathVariable Long govornikId) {
        return ResponseEntity.ok(sesijaService.removeGovornikFromSesija(sesijaId, govornikId));
    }
}
