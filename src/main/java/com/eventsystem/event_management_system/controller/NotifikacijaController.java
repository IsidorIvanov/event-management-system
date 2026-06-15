package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.NotifikacijaResponseDto;
import com.eventsystem.event_management_system.service.NotifikacijaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifikacije")
@RequiredArgsConstructor
public class NotifikacijaController {

    private final NotifikacijaService notifikacijaService;

    @GetMapping("/moje")
    public ResponseEntity<List<NotifikacijaResponseDto>> getMoje() {
        return ResponseEntity.ok(notifikacijaService.getMojeNotifikacije());
    }

    @GetMapping("/neprocitane/broj")
    public ResponseEntity<Long> countNeprocitane() {
        return ResponseEntity.ok(notifikacijaService.countNeprocitane());
    }

    @PostMapping("/{id}/procitano")
    public ResponseEntity<NotifikacijaResponseDto> oznaciKaoProcitano(@PathVariable Long id) {
        return ResponseEntity.ok(notifikacijaService.oznaciKaoProcitano(id));
    }
}
