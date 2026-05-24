package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.GenerisiPorudzbenicuRequestDto;
import com.eventsystem.event_management_system.dto.PorudzbenicaDto;
import com.eventsystem.event_management_system.dto.StatusUpdateDto;
import com.eventsystem.event_management_system.service.PorudzbenicaService;
import com.eventsystem.event_management_system.utils.enums.StatusPorudzbenice;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @Valid @RequestBody StatusUpdateDto body
    ) {
        StatusPorudzbenice status;
        try {
            status = StatusPorudzbenice.valueOf(body.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Nepoznat status porudžbenice: " + body.getStatus());
        }
        return ResponseEntity.ok(porudzbenicaService.updateStatus(id, status));
    }
}
