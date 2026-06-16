package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.DodelaOpremeDto;
import com.eventsystem.event_management_system.dto.InventarKretanjeDto;
import com.eventsystem.event_management_system.dto.InventarPotrebeValidacijaDto;
import com.eventsystem.event_management_system.service.DodelaOpremeService;
import com.eventsystem.event_management_system.service.InventarPotrebeService;
import com.eventsystem.event_management_system.service.InventarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventar")
@RequiredArgsConstructor
public class InventarController {

    private final InventarService inventarService;
    private final DodelaOpremeService dodelaOpremeService;
    private final InventarPotrebeService inventarPotrebeService;

    @PostMapping("/prijem/porudzbenica/{porudzbenicaId}")
    public ResponseEntity<List<InventarKretanjeDto>> primiIzPorudzbenice(@PathVariable Long porudzbenicaId) {
        return ResponseEntity.ok(inventarService.primiIzPorudzbenice(porudzbenicaId));
    }

    @GetMapping("/prijem/porudzbenica/{porudzbenicaId}/status")
    public ResponseEntity<Map<String, Boolean>> statusPrijema(@PathVariable Long porudzbenicaId) {
        return ResponseEntity.ok(Map.of("primljeno", inventarService.jePrimljenaPorudzbenica(porudzbenicaId)));
    }

    @GetMapping("/kretanje/oprema/{opremaId}")
    public ResponseEntity<List<InventarKretanjeDto>> kretanjaZaOprema(@PathVariable Long opremaId) {
        return ResponseEntity.ok(inventarService.getKretanjaZaOprema(opremaId));
    }

    @GetMapping("/dodela/dogadjaj/{dogadjajId}")
    public ResponseEntity<List<DodelaOpremeDto>> dodelePoDogadjaju(@PathVariable Long dogadjajId) {
        return ResponseEntity.ok(dodelaOpremeService.getByDogadjaj(dogadjajId));
    }

    @GetMapping("/dodela/{id}")
    public ResponseEntity<DodelaOpremeDto> dodelaById(@PathVariable Long id) {
        return ResponseEntity.ok(dodelaOpremeService.getById(id));
    }

    @PostMapping("/dodela")
    public ResponseEntity<DodelaOpremeDto> rezervisi(@Valid @RequestBody DodelaOpremeDto dto) {
        return ResponseEntity.ok(dodelaOpremeService.rezervisi(dto));
    }

    @PatchMapping("/dodela/{id}/aktiviraj")
    public ResponseEntity<DodelaOpremeDto> aktiviraj(@PathVariable Long id) {
        return ResponseEntity.ok(dodelaOpremeService.aktiviraj(id));
    }

    @PatchMapping("/dodela/{id}/oslobodi")
    public ResponseEntity<DodelaOpremeDto> oslobodi(@PathVariable Long id) {
        return ResponseEntity.ok(dodelaOpremeService.oslobodi(id));
    }

    @PatchMapping("/dodela/{id}/otkazi")
    public ResponseEntity<DodelaOpremeDto> otkazi(@PathVariable Long id) {
        return ResponseEntity.ok(dodelaOpremeService.otkazi(id));
    }

    @GetMapping("/potrebe/dogadjaj/{dogadjajId}")
    public ResponseEntity<InventarPotrebeValidacijaDto> validirajPotrebe(@PathVariable Long dogadjajId) {
        return ResponseEntity.ok(inventarPotrebeService.validirajPotrebeInventarom(dogadjajId));
    }
}
