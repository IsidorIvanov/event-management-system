package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.DobavljacDto;
import com.eventsystem.event_management_system.service.DobavljacService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nabavka/dobavljac")
@RequiredArgsConstructor
public class DobavljacController {

    private final DobavljacService dobavljacService;

    @GetMapping("")
    public ResponseEntity<List<DobavljacDto>> getAll() {
        return ResponseEntity.ok(dobavljacService.getAll());
    }

    @GetMapping("/aktivni")
    public ResponseEntity<List<DobavljacDto>> getAktivni() {
        return ResponseEntity.ok(dobavljacService.getAktivni());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DobavljacDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(dobavljacService.getById(id));
    }

    @PostMapping("")
    public ResponseEntity<DobavljacDto> create(@Valid @RequestBody DobavljacDto dto) {
        return ResponseEntity.ok(dobavljacService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DobavljacDto> update(@PathVariable Long id, @Valid @RequestBody DobavljacDto dto) {
        return ResponseEntity.ok(dobavljacService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        dobavljacService.delete(id);
        return ResponseEntity.ok("Dobavljac uspesno obrisan.");
    }
}
