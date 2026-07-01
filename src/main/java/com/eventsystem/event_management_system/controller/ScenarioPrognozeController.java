package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.CreateScenarioPrognozeRequest;
import com.eventsystem.event_management_system.dto.ScenarioPrognozeDto;
import com.eventsystem.event_management_system.dto.ScenarioPrognozeMetricsDto;
import com.eventsystem.event_management_system.service.ScenarioPrognozeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analiza/scenariji-prognoze")
@RequiredArgsConstructor
public class ScenarioPrognozeController {

    private final ScenarioPrognozeService scenarioPrognozeService;

    @PostMapping("/dogadjaj/{dogadjajId}")
    public ResponseEntity<ScenarioPrognozeDto> create(
            @PathVariable Long dogadjajId,
            @Valid @RequestBody CreateScenarioPrognozeRequest request
    ) {
        return ResponseEntity.ok(scenarioPrognozeService.create(dogadjajId, request));
    }

    @GetMapping("/dogadjaj/{dogadjajId}")
    public ResponseEntity<List<ScenarioPrognozeDto>> getByDogadjaj(@PathVariable Long dogadjajId) {
        return ResponseEntity.ok(scenarioPrognozeService.getByDogadjaj(dogadjajId));
    }

    @GetMapping("/{scenarioId}/metrics")
    public ResponseEntity<ScenarioPrognozeMetricsDto> getMetrics(@PathVariable Long scenarioId) {
        return ResponseEntity.ok(scenarioPrognozeService.getMetrics(scenarioId));
    }
}
