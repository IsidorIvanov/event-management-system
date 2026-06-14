package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.ProfilDto;
import com.eventsystem.event_management_system.dto.ProfilUpdateRequest;
import com.eventsystem.event_management_system.service.ProfilService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profil")
@RequiredArgsConstructor
public class ProfilController {

    private final ProfilService profilService;

    @GetMapping
    public ResponseEntity<?> getMojProfil() {
        try {
            return ResponseEntity.ok(profilService.getMojProfil());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> azurirajMojProfil(@Valid @RequestBody ProfilUpdateRequest request) {
        try {
            ProfilDto azuriran = profilService.azurirajMojProfil(request);
            return ResponseEntity.ok(azuriran);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
