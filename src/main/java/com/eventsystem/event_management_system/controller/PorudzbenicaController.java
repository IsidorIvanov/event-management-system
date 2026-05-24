package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.GenerisiPorudzbenicuRequestDto;
import com.eventsystem.event_management_system.dto.PorudzbenicaDto;
import com.eventsystem.event_management_system.service.PorudzbenicaService;
import com.eventsystem.event_management_system.utils.enums.StatusPorudzbenice;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nabavka/porudzbenica")
@RequiredArgsConstructor
public class PorudzbenicaController {

    private final PorudzbenicaService porudzbenicaService;

    @GetMapping("")
    public ResponseEntity<List<PorudzbenicaDto>> getAll() {
        return ResponseEntity.ok(porudzbenicaService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PorudzbenicaDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(porudzbenicaService.getById(id));
    }

    @GetMapping("/dogadjaj/{dogadjajId}")
    public ResponseEntity<List<PorudzbenicaDto>> getByDogadjaj(@PathVariable Long dogadjajId) {
        return ResponseEntity.ok(porudzbenicaService.getByDogadjaj(dogadjajId));
    }

    @PostMapping("/generisi")
    public ResponseEntity<PorudzbenicaDto> generisi(@Valid @RequestBody GenerisiPorudzbenicuRequestDto request) {
        return ResponseEntity.ok(porudzbenicaService.generisiIzNabavke(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PorudzbenicaDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, StatusPorudzbenice> body
    ) {
        StatusPorudzbenice status = body.get("status");
        if (status == null) {
            throw new RuntimeException("Polje 'status' je obavezno.");
        }
        return ResponseEntity.ok(porudzbenicaService.updateStatus(id, status));
    }
}
