package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.DogadjajDto;
import com.eventsystem.event_management_system.dto.DogadjajResponseDto;
import com.eventsystem.event_management_system.service.DogadjajService;
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

    @GetMapping("")
    public ResponseEntity<List<DogadjajResponseDto>> getAllDogadjaji() {
        return ResponseEntity.ok(dogadjajService.getAllDogadjaji());
    }

    @PostMapping("")
    public ResponseEntity<DogadjajResponseDto> createDogadjaj(@Valid @RequestBody DogadjajDto dto) {
        return ResponseEntity.ok(dogadjajService.saveDogadjaj(dto));
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
