package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.AnalizaProfitabilnostiDto;
import com.eventsystem.event_management_system.dto.CreateAnalizaProfitabilnostiRequest;
import com.eventsystem.event_management_system.dto.UpdateAnalizaProfitabilnostiRequest;
import com.eventsystem.event_management_system.service.AnalizaProfitabilnostiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analiza/profitabilnost")
public class AnalizaProfitabilnostiController {

    private final AnalizaProfitabilnostiService analizaProfitabilnostiService;

    @PostMapping("/dogadjaj/{dogadjajId}")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalizaProfitabilnostiDto createDraft(
            @PathVariable Long dogadjajId,
            @Valid @RequestBody(required = false) CreateAnalizaProfitabilnostiRequest request
    ) {
        return analizaProfitabilnostiService.createDraft(dogadjajId, request);
    }

    @PutMapping("/{analizaId}/finalizuj")
    public AnalizaProfitabilnostiDto finalize(@PathVariable Long analizaId) {
        return analizaProfitabilnostiService.finalize(analizaId);
    }

    @PutMapping("/{analizaId}")
    public AnalizaProfitabilnostiDto updateDraft(
            @PathVariable Long analizaId,
            @Valid @RequestBody(required = false) UpdateAnalizaProfitabilnostiRequest request
    ) {
        return analizaProfitabilnostiService.updateDraft(analizaId, request);
    }

    @GetMapping("/{analizaId}")
    public AnalizaProfitabilnostiDto getById(@PathVariable Long analizaId) {
        return analizaProfitabilnostiService.getById(analizaId);
    }

    @GetMapping("/dogadjaj/{dogadjajId}")
    public List<AnalizaProfitabilnostiDto> getByDogadjaj(@PathVariable Long dogadjajId) {
        return analizaProfitabilnostiService.getByDogadjaj(dogadjajId);
    }
}
