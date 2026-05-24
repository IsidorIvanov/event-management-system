package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.LokacijaDto;
import com.eventsystem.event_management_system.service.LokacijaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lokacija")
@RequiredArgsConstructor
public class LokacijaController {

    private final LokacijaService lokacijaService;

    @GetMapping("")
    public ResponseEntity<List<LokacijaDto>> getAllLokacije() {
        return ResponseEntity.ok(lokacijaService.getAllLokacije());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LokacijaDto> getLokacijaById(@PathVariable Long id) {
        return ResponseEntity.ok(lokacijaService.getLokacijaById(id));
    }

    @PostMapping("")
    public ResponseEntity<LokacijaDto> createLokacija(@Valid @RequestBody LokacijaDto lokacijaDto) {
        return ResponseEntity.ok(lokacijaService.saveLokacija(lokacijaDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LokacijaDto> updateLokacija(@PathVariable Long id, @Valid @RequestBody LokacijaDto lokacijaDto) {
        return ResponseEntity.ok(lokacijaService.updateLokacija(id, lokacijaDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteLokacija(@PathVariable Long id) {
        lokacijaService.deleteLokacija(id);
        return ResponseEntity.ok("Lokacija sa id " + id + " uspesno obrisana.");
    }
}
