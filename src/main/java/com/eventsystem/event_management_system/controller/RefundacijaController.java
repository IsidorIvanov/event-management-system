package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.RefundacijaDto;
import com.eventsystem.event_management_system.model.Refundacija;
import com.eventsystem.event_management_system.service.RefundacijaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refundacije")
@RequiredArgsConstructor
public class RefundacijaController {

    private final RefundacijaService refundacijaService;

    @PostMapping("/placanje/{placanjeId}")
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<Refundacija> request(@PathVariable Long placanjeId, @RequestBody RefundacijaDto dto,
                                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(refundacijaService.request(placanjeId, dto, userId));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    public ResponseEntity<Refundacija> approve(@PathVariable Long id,
                                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(refundacijaService.approve(id, userId));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    public ResponseEntity<Refundacija> reject(@PathVariable Long id,
                                              @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(refundacijaService.reject(id, userId));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<Refundacija> execute(@PathVariable Long id) {
        return ResponseEntity.ok(refundacijaService.execute(id));
    }
}
