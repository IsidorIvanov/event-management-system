package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanAlokacijeDto {

    private BigDecimal ukupnaCena;
    private int brojDobavljaca;
    private boolean svePokriveno;

    @Builder.Default
    private List<StavkaAlokacijeDto> stavke = new ArrayList<>();

    @Builder.Default
    private List<String> upozorenja = new ArrayList<>();
}
