package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.DogadjajDto;
import com.eventsystem.event_management_system.dto.DogadjajResponseDto;
import com.eventsystem.event_management_system.dto.SredstvaDogadjajaDto;
import com.eventsystem.event_management_system.dto.ZauzetiTerminDto;
import com.eventsystem.event_management_system.service.DogadjajService;
import com.eventsystem.event_management_system.service.SredstvaDogadjajaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dogadjaj")
public class DogadjajController {

    private final DogadjajService dogadjajService;
    private final SredstvaDogadjajaService sredstvaDogadjajaService;

    @GetMapping("")
    public ResponseEntity<List<DogadjajResponseDto>> getAllDogadjaji() {
        return ResponseEntity.ok(dogadjajService.getAllDogadjaji());
    }

    @PostMapping("")
    public ResponseEntity<DogadjajResponseDto> createDogadjaj(@Valid @RequestBody DogadjajDto dto) {
        return ResponseEntity.ok(dogadjajService.saveDogadjaj(dto));
    }

    @GetMapping("/zauzeti-termini")
    public ResponseEntity<List<ZauzetiTerminDto>> getZauzetiTermini(
            @RequestParam Long lokacijaId,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(dogadjajService.getZauzetiTermini(lokacijaId, excludeId));
    }

    @GetMapping("/{id}/sredstva")
    public ResponseEntity<SredstvaDogadjajaDto> getSredstva(@PathVariable Long id) {
        return ResponseEntity.ok(sredstvaDogadjajaService.getSredstva(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DogadjajResponseDto> getDogadjaj(@PathVariable Long id) {
        return ResponseEntity.ok(dogadjajService.getDogadjajById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DogadjajResponseDto> updateDogadjaj(@PathVariable Long id, @Valid @RequestBody DogadjajDto dto) {
        return ResponseEntity.ok(dogadjajService.updateDogadjaj(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDogadjaj(@PathVariable Long id) {
        dogadjajService.deleteDogadjaj(id);
        return ResponseEntity.ok("Dogadjaj with id " + id + " deleted successfully.");
    }
}
