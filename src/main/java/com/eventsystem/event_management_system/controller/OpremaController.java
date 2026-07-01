package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.OpremaDto;
import com.eventsystem.event_management_system.service.OpremaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventar/oprema")
@RequiredArgsConstructor
public class OpremaController {

    private final OpremaService opremaService;

    @GetMapping("")
    public ResponseEntity<List<OpremaDto>> getAll() {
        return ResponseEntity.ok(opremaService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OpremaDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(opremaService.getById(id));
    }

    @PostMapping("")
    public ResponseEntity<OpremaDto> create(@Valid @RequestBody OpremaDto dto) {
        return ResponseEntity.ok(opremaService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OpremaDto> update(@PathVariable Long id, @Valid @RequestBody OpremaDto dto) {
        return ResponseEntity.ok(opremaService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        opremaService.delete(id);
        return ResponseEntity.ok("Oprema uspešno obrisana.");
    }
}
