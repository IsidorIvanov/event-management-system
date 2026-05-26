package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.BudzetDto;
import com.eventsystem.event_management_system.dto.BudzetAlertDto;
import com.eventsystem.event_management_system.dto.StavkaBudzetaDto;
import com.eventsystem.event_management_system.service.BudzetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/budzet")
@RequiredArgsConstructor
public class BudzetController {

    private final BudzetService budzetService;

    @GetMapping("")
    public ResponseEntity<List<BudzetDto>> getAll() {
        return ResponseEntity.ok(budzetService.getAll());
    }

    @GetMapping("/alerts/active")
    public ResponseEntity<List<BudzetAlertDto>> getActiveAlerts() {
        return ResponseEntity.ok(budzetService.getActiveAlerts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudzetDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(budzetService.getById(id));
    }

    @GetMapping("/{id}/alerts")
    public ResponseEntity<List<BudzetAlertDto>> getAlerts(@PathVariable Long id) {
        return ResponseEntity.ok(budzetService.getAlerts(id));
    }

    @GetMapping("/dogadjaj/{dogadjajId}")
    public ResponseEntity<List<BudzetDto>> getByDogadjaj(@PathVariable Long dogadjajId) {
        return ResponseEntity.ok(budzetService.getByDogadjaj(dogadjajId));
    }

    @PostMapping("")
    public ResponseEntity<BudzetDto> create(@Valid @RequestBody BudzetDto dto) {
        return ResponseEntity.ok(budzetService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudzetDto> update(@PathVariable Long id, @Valid @RequestBody BudzetDto dto) {
        return ResponseEntity.ok(budzetService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        budzetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/stavke")
    public ResponseEntity<BudzetDto> addStavka(@PathVariable Long id, @Valid @RequestBody StavkaBudzetaDto dto) {
        return ResponseEntity.ok(budzetService.addStavka(id, dto));
    }

    @PutMapping("/{id}/stavke/{kategorijaId}")
    public ResponseEntity<BudzetDto> updateStavka(
            @PathVariable Long id,
            @PathVariable Long kategorijaId,
            @Valid @RequestBody StavkaBudzetaDto dto
    ) {
        return ResponseEntity.ok(budzetService.updateStavka(id, kategorijaId, dto));
    }

    @DeleteMapping("/{id}/stavke/{kategorijaId}")
    public ResponseEntity<BudzetDto> deleteStavka(@PathVariable Long id, @PathVariable Long kategorijaId) {
        return ResponseEntity.ok(budzetService.deleteStavka(id, kategorijaId));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<BudzetDto> approve(@PathVariable Long id) {
        return ResponseEntity.ok(budzetService.approve(id));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<BudzetDto> activate(@PathVariable Long id) {
        return ResponseEntity.ok(budzetService.activate(id));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<BudzetDto> close(@PathVariable Long id) {
        return ResponseEntity.ok(budzetService.close(id));
    }
}
