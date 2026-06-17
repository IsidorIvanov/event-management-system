package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.*;
import com.eventsystem.event_management_system.service.AlokacijaNabavkeService;
import com.eventsystem.event_management_system.service.DobavljacKontaktService;
import com.eventsystem.event_management_system.service.DobavljacSelekcijaService;
import com.eventsystem.event_management_system.service.NabavkaService;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nabavka")
@RequiredArgsConstructor
public class NabavkaController {

    private final NabavkaService nabavkaService;
    private final DobavljacSelekcijaService selekcijaService;
    private final DobavljacKontaktService kontaktService;
    private final AlokacijaNabavkeService alokacijaNabavkeService;

    @GetMapping("")
    public ResponseEntity<List<NabavkaDto>> getAll() {
        return ResponseEntity.ok(nabavkaService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NabavkaDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(nabavkaService.getById(id));
    }

    @GetMapping("/dogadjaj/{dogadjajId}")
    public ResponseEntity<List<NabavkaDto>> getByDogadjaj(@PathVariable Long dogadjajId) {
        return ResponseEntity.ok(nabavkaService.getByDogadjaj(dogadjajId));
    }

    @PostMapping("")
    public ResponseEntity<NabavkaDto> create(@Valid @RequestBody NabavkaDto dto) {
        return ResponseEntity.ok(nabavkaService.create(dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<NabavkaDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, StatusNabavke> body
    ) {
        StatusNabavke status = body.get("status");
        if (status == null) {
            throw new RuntimeException("Polje 'status' je obavezno.");
        }
        return ResponseEntity.ok(nabavkaService.updateStatus(id, status));
    }

    @PostMapping("/selekcija/predlog")
    public ResponseEntity<PredlogDobavljacaDto> predloziDobavljaca(
            @Valid @RequestBody AutomatskaSelekcijaRequestDto request
    ) {
        return ResponseEntity.ok(selekcijaService.predloziDobavljaca(request));
    }

    @PostMapping("/selekcija/predlog-detaljno")
    public ResponseEntity<SelekcijaDobavljacaResponseDto> predloziSaAlternativama(
            @Valid @RequestBody AutomatskaSelekcijaRequestDto request
    ) {
        return ResponseEntity.ok(selekcijaService.predloziSaAlternativama(request));
    }

    @PostMapping("/alokacija/optimizuj")
    public ResponseEntity<PlanAlokacijeDto> optimizujAlokaciju(
            @Valid @RequestBody OptimizujAlokacijuRequestDto request
    ) {
        return ResponseEntity.ok(alokacijaNabavkeService.optimizuj(request.getPotrebneStavke()));
    }

    @PostMapping("/alokacija/primeni")
    public ResponseEntity<PlanAlokacijeDto> primeniAlokaciju(
            @Valid @RequestBody OptimizujAlokacijuRequestDto request
    ) {
        if (request.getNabavkaId() == null) {
            throw new RuntimeException("ID nabavke je obavezan za primenu alokacije.");
        }
        return ResponseEntity.ok(alokacijaNabavkeService.primeni(request.getNabavkaId(), request.getPotrebneStavke()));
    }

    @PostMapping("/selekcija/primeni")
    public ResponseEntity<PredlogDobavljacaDto> primeniSelekciju(
            @Valid @RequestBody AutomatskaSelekcijaRequestDto request
    ) {
        return ResponseEntity.ok(selekcijaService.primeniAutomatskuSelekciju(request));
    }

    @GetMapping("/{id}/kontakt/pregled")
    public ResponseEntity<KontaktDobavljacaDto> pregledEmailUpita(@PathVariable Long id) {
        return ResponseEntity.ok(kontaktService.pripremiEmailUpit(id));
    }

    @PostMapping("/{id}/kontakt/posalji")
    public ResponseEntity<KontaktDobavljacaDto> posaljiEmailUpit(@PathVariable Long id) {
        return ResponseEntity.ok(kontaktService.posaljiEmailUpit(id));
    }

    @PostMapping("/{id}/kontakt/{dobavljacId}")
    public ResponseEntity<KontaktDobavljacaDto> kontaktirajDobavljaca(
            @PathVariable Long id,
            @PathVariable Long dobavljacId
    ) {
        return ResponseEntity.ok(kontaktService.kontaktirajDobavljaca(id, dobavljacId));
    }
}
