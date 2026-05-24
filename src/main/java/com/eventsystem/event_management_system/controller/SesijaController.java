package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.SesijaDto;
import com.eventsystem.event_management_system.service.SesijaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
