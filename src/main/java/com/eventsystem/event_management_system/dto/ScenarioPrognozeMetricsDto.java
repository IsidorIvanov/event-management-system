package com.eventsystem.event_management_system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioPrognozeMetricsDto {

    private Long scenarioId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal neto;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal marza;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal roi;
}
