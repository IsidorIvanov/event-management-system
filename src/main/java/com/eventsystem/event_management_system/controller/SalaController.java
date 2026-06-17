package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.*;
import com.eventsystem.event_management_system.service.AlokacijaSaleService;
import com.eventsystem.event_management_system.service.DinamickoCeneService;
import com.eventsystem.event_management_system.service.SalaDostupnostService;
import com.eventsystem.event_management_system.service.SalaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sala")
@RequiredArgsConstructor
public class SalaController {

    private final SalaService salaService;
    private final SalaDostupnostService salaDostupnostService;
    private final AlokacijaSaleService alokacijaSaleService;
    private final DinamickoCeneService dinamickoCeneService;

    @GetMapping("/dostupnost")
    public ResponseEntity<SalaDostupnostResponseDto> getDostupnost(
            @RequestParam Long lokacijaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datumOd,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datumDo) {
        return ResponseEntity.ok(salaDostupnostService.getDostupnost(lokacijaId, datumOd, datumDo));
    }

    @GetMapping("/predlog")
    public ResponseEntity<PredlogSalaResponseDto> predlogZaSesiju(@RequestParam Long sesijaId) {
        return ResponseEntity.ok(alokacijaSaleService.predlogZaSesiju(sesijaId));
    }

    @PostMapping("/predlog")
    public ResponseEntity<PredlogSalaResponseDto> predlogZaParametre(
            @Valid @RequestBody PredlogSalaRequestDto request) {
        return ResponseEntity.ok(alokacijaSaleService.predlogZaParametre(request));
    }

    @GetMapping("/{lokacijaId}/{nazivSale}/cena/predlog")
    public ResponseEntity<PredlogCeneSaleDto> predlogCeneSale(
            @PathVariable Long lokacijaId,
            @PathVariable String nazivSale,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datum) {
        return ResponseEntity.ok(dinamickoCeneService.predlogCeneSale(lokacijaId, nazivSale, datum));
    }

    @PatchMapping("/{lokacijaId}/{nazivSale}/cena")
    public ResponseEntity<PredlogCeneSaleDto> primeniCenuSale(
            @PathVariable Long lokacijaId,
            @PathVariable String nazivSale,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datum) {
        return ResponseEntity.ok(dinamickoCeneService.primeniCenuSale(lokacijaId, nazivSale, datum));
    }

    @GetMapping("")
    public ResponseEntity<List<SalaDto>> getSaleByLokacija(@RequestParam Long lokacijaId) {
        return ResponseEntity.ok(salaService.getSaleByLokacija(lokacijaId));
    }

    @GetMapping("/{lokacijaId}/{nazivSale}")
    public ResponseEntity<SalaDto> getSala(
            @PathVariable Long lokacijaId,
            @PathVariable String nazivSale) {
        return ResponseEntity.ok(salaService.getSala(lokacijaId, nazivSale));
    }

    @PostMapping("")
    public ResponseEntity<SalaDto> createSala(@Valid @RequestBody SalaDto salaDto) {
        return ResponseEntity.ok(salaService.createSala(salaDto));
    }

    @PutMapping("/{lokacijaId}/{nazivSale}")
    public ResponseEntity<SalaDto> updateSala(
            @PathVariable Long lokacijaId,
            @PathVariable String nazivSale,
            @Valid @RequestBody SalaUpdateDto salaDto) {
        return ResponseEntity.ok(salaService.updateSala(lokacijaId, nazivSale, salaDto));
    }

    @DeleteMapping("/{lokacijaId}/{nazivSale}")
    public ResponseEntity<String> deleteSala(
            @PathVariable Long lokacijaId,
            @PathVariable String nazivSale) {
        salaService.deleteSala(lokacijaId, nazivSale);
        return ResponseEntity.ok("Sala '" + nazivSale + "' na lokaciji " + lokacijaId + " uspesno obrisana.");
    }
}
