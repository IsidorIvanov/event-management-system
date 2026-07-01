package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.CreateScenarioPrognozeRequest;
import com.eventsystem.event_management_system.dto.ScenarioPrognozeDto;
import com.eventsystem.event_management_system.dto.ScenarioPrognozeMetricsDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.ScenarioPrognoze;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.ScenarioPrognozeRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import com.eventsystem.event_management_system.utils.enums.TipScenarija;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScenarioPrognozeServiceTest {

    @Mock
    private ScenarioPrognozeRepository scenarioPrognozeRepository;

    @Mock
    private DogadjajRepository dogadjajRepository;

    @InjectMocks
    private ScenarioPrognozeService scenarioPrognozeService;

    @Test
    void create_successCreatesScenarioForEventThatIsNotFinished() {
        CreateScenarioPrognozeRequest request = validRequest();
        when(dogadjajRepository.findById(10L)).thenReturn(Optional.of(dogadjaj(StatusDogadjaja.AKTIVAN)));
        when(scenarioPrognozeRepository.save(any(ScenarioPrognoze.class))).thenAnswer(invocation -> {
            ScenarioPrognoze scenario = invocation.getArgument(0);
            scenario.setScenarioId(99L);
            scenario.setCreatedAt(LocalDateTime.now());
            return scenario;
        });

        ScenarioPrognozeDto dto = scenarioPrognozeService.create(10L, request);

        assertThat(dto.getScenarioId()).isEqualTo(99L);
        assertThat(dto.getDogadjajId()).isEqualTo(10L);
        assertThat(dto.getNazivScenarija()).isEqualTo("Realistična prognoza");
        assertThat(dto.getTipScenarija()).isEqualTo(TipScenarija.REALISTICNI);
        assertThat(dto.getProjektovaniPrihod()).isEqualByComparingTo("1000.00");
        assertThat(dto.getProjektovaniTrosak()).isEqualByComparingTo("650.00");
    }

    @Test
    void create_rejectsFinishedEvent() {
        when(dogadjajRepository.findById(10L)).thenReturn(Optional.of(dogadjaj(StatusDogadjaja.ZAVRSEN)));

        assertThatThrownBy(() -> scenarioPrognozeService.create(10L, validRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("završen");
    }

    @Test
    void create_rejectsNegativeProjectedRevenue() {
        CreateScenarioPrognozeRequest request = validRequest();
        request.setProjektovaniPrihod(new BigDecimal("-0.01"));

        assertThatThrownBy(() -> scenarioPrognozeService.create(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("negativan");
    }

    @Test
    void create_rejectsNegativeProjectedCost() {
        CreateScenarioPrognozeRequest request = validRequest();
        request.setProjektovaniTrosak(new BigDecimal("-0.01"));

        assertThatThrownBy(() -> scenarioPrognozeService.create(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("negativan");
    }

    @Test
    void create_rejectsBlankScenarioName() {
        CreateScenarioPrognozeRequest request = validRequest();
        request.setNazivScenarija("  ");

        assertThatThrownBy(() -> scenarioPrognozeService.create(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Naziv scenarija");
    }

    @Test
    void getMetrics_calculatesNetMarginAndRoi() {
        ScenarioPrognoze scenario = scenario(new BigDecimal("1000.00"), new BigDecimal("650.00"));
        when(scenarioPrognozeRepository.findByIdWithDogadjaj(5L)).thenReturn(Optional.of(scenario));

        ScenarioPrognozeMetricsDto metrics = scenarioPrognozeService.getMetrics(5L);

        assertThat(metrics.getScenarioId()).isEqualTo(5L);
        assertThat(metrics.getNeto()).isEqualByComparingTo("350.00");
        assertThat(metrics.getMarza()).isEqualByComparingTo("0.3500");
        assertThat(metrics.getRoi()).isEqualByComparingTo("0.5385");
    }

    @Test
    void getMetrics_returnsNullMarginWhenRevenueIsZero() {
        ScenarioPrognoze scenario = scenario(BigDecimal.ZERO, new BigDecimal("100.00"));
        when(scenarioPrognozeRepository.findByIdWithDogadjaj(5L)).thenReturn(Optional.of(scenario));

        ScenarioPrognozeMetricsDto metrics = scenarioPrognozeService.getMetrics(5L);

        assertThat(metrics.getNeto()).isEqualByComparingTo("-100.00");
        assertThat(metrics.getMarza()).isNull();
        assertThat(metrics.getRoi()).isEqualByComparingTo("-1.0000");
    }

    @Test
    void getMetrics_returnsNullRoiWhenCostIsZero() {
        ScenarioPrognoze scenario = scenario(new BigDecimal("100.00"), BigDecimal.ZERO);
        when(scenarioPrognozeRepository.findByIdWithDogadjaj(5L)).thenReturn(Optional.of(scenario));

        ScenarioPrognozeMetricsDto metrics = scenarioPrognozeService.getMetrics(5L);

        assertThat(metrics.getNeto()).isEqualByComparingTo("100.00");
        assertThat(metrics.getMarza()).isEqualByComparingTo("1.0000");
        assertThat(metrics.getRoi()).isNull();
    }

    private CreateScenarioPrognozeRequest validRequest() {
        return CreateScenarioPrognozeRequest.builder()
                .nazivScenarija("Realistična prognoza")
                .tipScenarija(TipScenarija.REALISTICNI)
                .projektovaniPrihod(new BigDecimal("1000.00"))
                .projektovaniTrosak(new BigDecimal("650.00"))
                .pretpostavke("Prodaja 80% kapaciteta")
                .build();
    }

    private Dogadjaj dogadjaj(StatusDogadjaja status) {
        return Dogadjaj.builder()
                .dogadjajId(10L)
                .naziv("Test događaj")
                .status(status)
                .build();
    }

    private ScenarioPrognoze scenario(BigDecimal prihod, BigDecimal trosak) {
        return ScenarioPrognoze.builder()
                .scenarioId(5L)
                .dogadjaj(dogadjaj(StatusDogadjaja.AKTIVAN))
                .nazivScenarija("Test scenario")
                .tipScenarija(TipScenarija.REALISTICNI)
                .projektovaniPrihod(prihod)
                .projektovaniTrosak(trosak)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
