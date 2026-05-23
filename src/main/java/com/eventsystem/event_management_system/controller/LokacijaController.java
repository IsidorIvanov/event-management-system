package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.LokacijaDto;
import com.eventsystem.event_management_system.service.LokacijaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lokacija")
@RequiredArgsConstructor
public class LokacijaController {

    private final LokacijaService lokacijaService;

    @PostMapping("")
    public ResponseEntity<LokacijaDto> addLokacija(@RequestBody LokacijaDto lokacijaDto) {
        return ResponseEntity.ok(lokacijaService.addLokacija(lokacijaDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LokacijaDto> updateLokacija(@PathVariable Long id, @RequestBody LokacijaDto lokacijaDto) {
        return ResponseEntity.ok(lokacijaService.updateLokacija(id, lokacijaDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteLokacija(@PathVariable Long id) {
        lokacijaService.deleteLokacija(id);
        return ResponseEntity.ok("Lokacija with id " + id + " deleted successfully.");
    }
}
