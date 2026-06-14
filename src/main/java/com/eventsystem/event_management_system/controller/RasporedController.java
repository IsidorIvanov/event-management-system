package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.RasporedSesijaDto;
import com.eventsystem.event_management_system.service.RasporedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/raspored")
@RequiredArgsConstructor
public class RasporedController {

    private final RasporedService rasporedService;

    @GetMapping("/moj")
    public ResponseEntity<List<RasporedSesijaDto>> getMojRaspored() {
        return ResponseEntity.ok(rasporedService.getMojRaspored());
    }

    @GetMapping("/moj/ids")
    public ResponseEntity<List<Long>> getMojRasporedIds() {
        return ResponseEntity.ok(rasporedService.getMojRasporedIds());
    }

    @PostMapping("/{sesijaId}")
    public ResponseEntity<RasporedSesijaDto> addSesija(@PathVariable Long sesijaId) {
        return ResponseEntity.ok(rasporedService.addSesijaToRaspored(sesijaId));
    }

    @DeleteMapping("/{sesijaId}")
    public ResponseEntity<Void> removeSesija(@PathVariable Long sesijaId) {
        rasporedService.removeSesijaFromRaspored(sesijaId);
        return ResponseEntity.noContent().build();
    }
}

