package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.AttachDocumentRequest;
import com.eventsystem.event_management_system.dto.CreateUgovorRequest;
import com.eventsystem.event_management_system.dto.UgovorDto;
import com.eventsystem.event_management_system.service.UgovorService;
import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UgovorController {

    private final UgovorService ugovorService;

    @GetMapping("/api/ugovori/aktivni")
    @PreAuthorize("hasAnyRole('MENADZER_DOGADJAJA', 'FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<List<UgovorDto>> getAktivni(@RequestParam Long dobavljacId) {
        return ResponseEntity.ok(ugovorService.getActiveByDobavljac(dobavljacId));
    }

    @GetMapping("/api/ugovori")
    @PreAuthorize("hasAnyRole('MENADZER_DOGADJAJA', 'FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<List<UgovorDto>> getAll(
            @RequestParam(required = false) Long dobavljacId,
            @RequestParam(required = false) Long dogadjajId,
            @RequestParam(required = false) StatusUgovora status
    ) {
        return ResponseEntity.ok(ugovorService.getAll(dobavljacId, dogadjajId, status));
    }

    @GetMapping("/api/ugovori/{id}")
    @PreAuthorize("hasAnyRole('MENADZER_DOGADJAJA', 'FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<UgovorDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ugovorService.getById(id));
    }

    @PostMapping("/api/ugovori")
    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    public ResponseEntity<UgovorDto> create(@Valid @RequestBody CreateUgovorRequest request) {
        return ResponseEntity.ok(ugovorService.create(request));
    }

    @PutMapping("/api/ugovori/{id}/dokument")
    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    public ResponseEntity<UgovorDto> attachDocument(
            @PathVariable Long id,
            @Valid @RequestBody AttachDocumentRequest request
    ) {
        return ResponseEntity.ok(ugovorService.attachDocument(id, request));
    }

    @PutMapping("/api/ugovori/{id}/aktiviraj")
    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    public ResponseEntity<UgovorDto> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ugovorService.activate(id));
    }

    @PutMapping("/api/ugovori/{id}/raskini")
    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    public ResponseEntity<UgovorDto> terminate(@PathVariable Long id) {
        return ResponseEntity.ok(ugovorService.terminate(id));
    }

    @GetMapping("/api/nabavka/ugovor/dobavljac/{dobavljacId}")
    @PreAuthorize("hasAnyRole('MENADZER_DOGADJAJA', 'FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<List<UgovorDto>> getByDobavljacLegacy(@PathVariable Long dobavljacId) {
        return ResponseEntity.ok(ugovorService.getByDobavljac(dobavljacId));
    }
}
