package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.UgovorDto;
import com.eventsystem.event_management_system.service.UgovorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nabavka/ugovor")
@RequiredArgsConstructor
public class UgovorController {

    private final UgovorService ugovorService;

    @GetMapping("/dobavljac/{dobavljacId}")
    public ResponseEntity<List<UgovorDto>> getByDobavljac(@PathVariable Long dobavljacId) {
        return ResponseEntity.ok(ugovorService.getByDobavljac(dobavljacId));
    }

    @PostMapping("")
    public ResponseEntity<UgovorDto> create(@Valid @RequestBody UgovorDto dto) {
        return ResponseEntity.ok(ugovorService.create(dto));
    }
}
