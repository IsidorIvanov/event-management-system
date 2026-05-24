package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.DogadjajDto;
import com.eventsystem.event_management_system.service.DogadjajService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dogadjaj")
public class DogadjajController {

    private final DogadjajService dogadjajService;

    @PostMapping("")
    public ResponseEntity<DogadjajDto> createDogadjaj(@RequestBody DogadjajDto dto) {
        return ResponseEntity.ok(dogadjajService.saveDogadjaj(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DogadjajDto> updateDogadjaj(@PathVariable Long id, @RequestBody DogadjajDto dto) {
        return ResponseEntity.ok(dogadjajService.updateDogadjaj(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDogadjaj(@PathVariable Long id) {
        dogadjajService.deleteDogadjaj(id);
        return ResponseEntity.ok("Dogadjaj with id " + id + " deleted successfully.");
    }
}
