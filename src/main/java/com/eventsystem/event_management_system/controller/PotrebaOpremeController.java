package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.DetektovanaPotrebaDto;
import com.eventsystem.event_management_system.dto.PotrebnaStavkaDto;
import com.eventsystem.event_management_system.dto.ValidacijaPotrebaDto;
import com.eventsystem.event_management_system.service.PotrebaOpremeDetekcijaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nabavka/potrebe")
@RequiredArgsConstructor
public class PotrebaOpremeController {

    private final PotrebaOpremeDetekcijaService detekcijaService;

    @GetMapping("/dogadjaj/{dogadjajId}")
    public ResponseEntity<List<DetektovanaPotrebaDto>> detektuj(@PathVariable Long dogadjajId) {
        return ResponseEntity.ok(detekcijaService.detektujPotrebe(dogadjajId));
    }

    @GetMapping("/dogadjaj/{dogadjajId}/validacija")
    public ResponseEntity<ValidacijaPotrebaDto> validirajDetektovane(@PathVariable Long dogadjajId) {
        return ResponseEntity.ok(detekcijaService.validirajPotrebe(dogadjajId));
    }

    @PostMapping("/dogadjaj/{dogadjajId}/validacija")
    public ResponseEntity<ValidacijaPotrebaDto> validirajUnete(
            @PathVariable Long dogadjajId,
            @Valid @RequestBody List<PotrebnaStavkaDto> stavke
    ) {
        return ResponseEntity.ok(detekcijaService.validirajUnetePotrebe(dogadjajId, stavke));
    }
}
