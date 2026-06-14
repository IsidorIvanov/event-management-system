package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.CreateScenarioPrognozeRequest;
import com.eventsystem.event_management_system.dto.ScenarioPrognozeDto;
import com.eventsystem.event_management_system.dto.ScenarioPrognozeMetricsDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.ScenarioPrognoze;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.ScenarioPrognozeRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScenarioPrognozeService {

    private static final int MONEY_SCALE = 2;
    private static final int RATIO_SCALE = 4;

    private final ScenarioPrognozeRepository scenarioPrognozeRepository;
    private final DogadjajRepository dogadjajRepository;

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional
    public ScenarioPrognozeDto create(Long dogadjajId, CreateScenarioPrognozeRequest request) {
        if (dogadjajId == null) {
            throw new BadRequestException("ID događaja je obavezan.");
        }
        validateCreateRequest(request);

        Dogadjaj dogadjaj = dogadjajRepository.findById(dogadjajId)
                .orElseThrow(() -> new NotFoundException("Događaj nije pronađen sa id: " + dogadjajId));

        if (dogadjaj.getStatus() == StatusDogadjaja.ZAVRSEN) {
            throw new BadRequestException("Scenario prognoze nije moguće kreirati za završen događaj.");
        }

        ScenarioPrognoze scenario = ScenarioPrognoze.builder()
                .dogadjaj(dogadjaj)
                .nazivScenarija(normalizeRequired(request.getNazivScenarija(), "Naziv scenarija"))
                .tipScenarija(request.getTipScenarija())
                .projektovaniPrihod(toMoney(request.getProjektovaniPrihod(), "Projektovani prihod"))
                .projektovaniTrosak(toMoney(request.getProjektovaniTrosak(), "Projektovani trošak"))
                .pretpostavke(normalizeNullable(request.getPretpostavke()))
                .build();

        return toDto(scenarioPrognozeRepository.save(scenario));
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public List<ScenarioPrognozeDto> getByDogadjaj(Long dogadjajId) {
        if (dogadjajId == null) {
            throw new BadRequestException("ID događaja je obavezan.");
        }
        return scenarioPrognozeRepository.findByDogadjajDogadjajId(dogadjajId).stream()
                .map(this::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public ScenarioPrognozeMetricsDto getMetrics(Long scenarioId) {
        ScenarioPrognoze scenario = findEntity(scenarioId);
        BigDecimal prihod = scenario.getProjektovaniPrihod();
        BigDecimal trosak = scenario.getProjektovaniTrosak();
        BigDecimal neto = prihod.subtract(trosak).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);

        return ScenarioPrognozeMetricsDto.builder()
                .scenarioId(scenario.getScenarioId())
                .neto(neto)
                .marza(prihod.compareTo(BigDecimal.ZERO) > 0
                        ? neto.divide(prihod, RATIO_SCALE, RoundingMode.HALF_EVEN)
                        : null)
                .roi(trosak.compareTo(BigDecimal.ZERO) > 0
                        ? neto.divide(trosak, RATIO_SCALE, RoundingMode.HALF_EVEN)
                        : null)
                .build();
    }

    private ScenarioPrognoze findEntity(Long scenarioId) {
        if (scenarioId == null) {
            throw new BadRequestException("ID scenarija je obavezan.");
        }
        return scenarioPrognozeRepository.findByIdWithDogadjaj(scenarioId)
                .orElseThrow(() -> new NotFoundException("Scenario prognoze nije pronađen sa id: " + scenarioId));
    }

    private void validateCreateRequest(CreateScenarioPrognozeRequest request) {
        if (request == null) {
            throw new BadRequestException("Podaci za scenario prognoze su obavezni.");
        }
        normalizeRequired(request.getNazivScenarija(), "Naziv scenarija");
        if (request.getTipScenarija() == null) {
            throw new BadRequestException("Tip scenarija je obavezan.");
        }
        toMoney(request.getProjektovaniPrihod(), "Projektovani prihod");
        toMoney(request.getProjektovaniTrosak(), "Projektovani trošak");
    }

    private ScenarioPrognozeDto toDto(ScenarioPrognoze scenario) {
        return ScenarioPrognozeDto.builder()
                .scenarioId(scenario.getScenarioId())
                .dogadjajId(scenario.getDogadjaj().getDogadjajId())
                .dogadjajNaziv(scenario.getDogadjaj().getNaziv())
                .nazivScenarija(scenario.getNazivScenarija())
                .tipScenarija(scenario.getTipScenarija())
                .projektovaniPrihod(scenario.getProjektovaniPrihod())
                .projektovaniTrosak(scenario.getProjektovaniTrosak())
                .pretpostavke(scenario.getPretpostavke())
                .createdAt(scenario.getCreatedAt())
                .build();
    }

    private BigDecimal toMoney(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " je obavezan.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(fieldName + " ne može biti negativan.");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " je obavezan.");
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : null;
    }
}
