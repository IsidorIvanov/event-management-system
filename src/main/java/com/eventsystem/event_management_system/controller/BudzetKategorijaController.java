package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.BudzetKategorijaDto;
import com.eventsystem.event_management_system.service.BudzetKategorijaService;
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
@RequestMapping("/api/budzet/kategorije")
@RequiredArgsConstructor
public class BudzetKategorijaController {

    private final BudzetKategorijaService kategorijaService;

    @GetMapping("")
    public ResponseEntity<List<BudzetKategorijaDto>> getAll() {
        return ResponseEntity.ok(kategorijaService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudzetKategorijaDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(kategorijaService.getById(id));
    }

    @PostMapping("")
    public ResponseEntity<BudzetKategorijaDto> create(@Valid @RequestBody BudzetKategorijaDto dto) {
        return ResponseEntity.ok(kategorijaService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudzetKategorijaDto> update(
            @PathVariable Long id,
            @Valid @RequestBody BudzetKategorijaDto dto
    ) {
        return ResponseEntity.ok(kategorijaService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        kategorijaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
