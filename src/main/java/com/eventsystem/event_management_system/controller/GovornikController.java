package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.GovornikDto;
import com.eventsystem.event_management_system.model.Govornik;
import com.eventsystem.event_management_system.service.GovornikService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/govornik")
@RequiredArgsConstructor
public class GovornikController {

    private final GovornikService govornikService;

    @PostMapping("")
    public ResponseEntity<GovornikDto> createGovornik(@Valid @RequestBody GovornikDto govornikDto) {
        return ResponseEntity.ok(govornikService.createGovornik(govornikDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GovornikDto> updateGovornik(@PathVariable Long id, @Valid @RequestBody GovornikDto govornikDto) {
        return ResponseEntity.ok(govornikService.updateGovornik(id, govornikDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGovornik(@PathVariable Long id) {
        govornikService.deleteGovornik(id);
        return ResponseEntity.ok("Govornik sa id " + id + " uspesno obrisan.");
    }
}
