package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanAlokacijeOpremeDto {

    private Long dogadjajId;
    private String dogadjajNaziv;
    private boolean svePokriveno;
    private int ukupnoPotreba;
    private int pokrivenoStavki;

    @Builder.Default
    private List<StavkaAlokacijeOpremeDto> stavke = new ArrayList<>();

    @Builder.Default
    private List<String> upozorenja = new ArrayList<>();
}
