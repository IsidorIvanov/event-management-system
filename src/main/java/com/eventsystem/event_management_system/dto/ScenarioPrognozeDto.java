package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.TipScenarija;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioPrognozeDto {

    private Long scenarioId;

    private Long dogadjajId;

    private String dogadjajNaziv;

    private String nazivScenarija;

    private TipScenarija tipScenarija;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal projektovaniPrihod;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal projektovaniTrosak;

    private String pretpostavke;

    private LocalDateTime createdAt;
}
