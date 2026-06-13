package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.RegistracijaDto;
import com.eventsystem.event_management_system.dto.RegistracijaResponseDto;
import com.eventsystem.event_management_system.service.RegistracijaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registracija")
@RequiredArgsConstructor
public class RegistracijaController {

    private final RegistracijaService registracijaService;

    @PostMapping("")
    public ResponseEntity<RegistracijaResponseDto> register(@Valid @RequestBody RegistracijaDto dto) {
        return ResponseEntity.ok(registracijaService.register(dto));
    }

    @GetMapping("/moje")
    public ResponseEntity<List<RegistracijaResponseDto>> getMyRegistrations() {
        return ResponseEntity.ok(registracijaService.getMyRegistrations());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RegistracijaResponseDto> cancelRegistration(@PathVariable Long id) {
        return ResponseEntity.ok(registracijaService.cancelRegistration(id));
    }
}

