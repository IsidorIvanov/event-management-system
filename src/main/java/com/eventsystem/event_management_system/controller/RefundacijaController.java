package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.RefundacijaDto;
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
    public ResponseEntity<RefundacijaDto> request(@PathVariable Long placanjeId, @RequestBody RefundacijaDto dto) {
        return ResponseEntity.ok(refundacijaService.request(placanjeId, dto));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    public ResponseEntity<RefundacijaDto> approve(@PathVariable Long id) {
        return ResponseEntity.ok(refundacijaService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    public ResponseEntity<RefundacijaDto> reject(@PathVariable Long id) {
        return ResponseEntity.ok(refundacijaService.reject(id));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public ResponseEntity<RefundacijaDto> execute(@PathVariable Long id) {
        return ResponseEntity.ok(refundacijaService.execute(id));
    }
}
