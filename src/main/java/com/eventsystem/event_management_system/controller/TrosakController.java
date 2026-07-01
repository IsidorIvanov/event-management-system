package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.TrosakDto;
import com.eventsystem.event_management_system.service.TrosakService;
import com.eventsystem.event_management_system.utils.enums.TipTroska;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/troskovi")
@RequiredArgsConstructor
public class TrosakController {

    private final TrosakService trosakService;

    @GetMapping("")
    public ResponseEntity<List<TrosakDto>> getAll(
            @RequestParam(required = false) Long dogadjajId,
            @RequestParam(required = false) Long budzetId,
            @RequestParam(required = false) Long kategorijaId,
            @RequestParam(required = false) TipTroska tip
    ) {
        return ResponseEntity.ok(trosakService.getAll(dogadjajId, budzetId, kategorijaId, tip));
    }

    @PostMapping("")
    public ResponseEntity<TrosakDto> createManual(@Valid @RequestBody TrosakDto dto) {
        return ResponseEntity.ok(trosakService.createManual(dto));
    }

    @GetMapping("/budzet/{budzetId}/kategorija/{kategorijaId}")
    public ResponseEntity<List<TrosakDto>> getByStavka(
            @PathVariable Long budzetId,
            @PathVariable Long kategorijaId
    ) {
        return ResponseEntity.ok(trosakService.getByStavka(budzetId, kategorijaId));
    }
}
