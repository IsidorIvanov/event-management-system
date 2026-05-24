package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.CenovnikDto;
import com.eventsystem.event_management_system.service.CenovnikService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nabavka/cenovnik")
@RequiredArgsConstructor
public class CenovnikController {

    private final CenovnikService cenovnikService;

    @GetMapping("")
    public ResponseEntity<List<CenovnikDto>> getAll() {
        return ResponseEntity.ok(cenovnikService.getAll());
    }

    @GetMapping("/dobavljac/{dobavljacId}")
    public ResponseEntity<List<CenovnikDto>> getByDobavljac(@PathVariable Long dobavljacId) {
        return ResponseEntity.ok(cenovnikService.getByDobavljac(dobavljacId));
    }

    @PostMapping("")
    public ResponseEntity<CenovnikDto> create(@Valid @RequestBody CenovnikDto dto) {
        return ResponseEntity.ok(cenovnikService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CenovnikDto> update(@PathVariable Long id, @Valid @RequestBody CenovnikDto dto) {
        return ResponseEntity.ok(cenovnikService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        cenovnikService.delete(id);
        return ResponseEntity.ok("Stavka cenovnika uspesno obrisana.");
    }
}
