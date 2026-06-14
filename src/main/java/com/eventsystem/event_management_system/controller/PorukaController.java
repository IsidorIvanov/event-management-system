package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.KontaktDto;
import com.eventsystem.event_management_system.dto.PorukaRequest;
import com.eventsystem.event_management_system.dto.PorukaResponseDto;
import com.eventsystem.event_management_system.service.PorukaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/poruke")
@RequiredArgsConstructor
public class PorukaController {

    private final PorukaService porukaService;

    @GetMapping("/kontakti")
    public ResponseEntity<List<KontaktDto>> getKontakti() {
        return ResponseEntity.ok(porukaService.getKontakti());
    }

    @GetMapping("/konverzacija/{korisnikId}")
    public ResponseEntity<List<PorukaResponseDto>> getKonverzacija(@PathVariable Long korisnikId) {
        return ResponseEntity.ok(porukaService.getKonverzacija(korisnikId));
    }

    @PostMapping("/posalji")
    public ResponseEntity<PorukaResponseDto> posaljiPoruku(@Valid @RequestBody PorukaRequest request) {
        return ResponseEntity.ok(porukaService.posaljiPoruku(request));
    }

    @GetMapping("/neprocitane")
    public ResponseEntity<Map<String, Long>> getNeprocitane() {
        return ResponseEntity.ok(Map.of("count", porukaService.getTotalUnread()));
    }
}

