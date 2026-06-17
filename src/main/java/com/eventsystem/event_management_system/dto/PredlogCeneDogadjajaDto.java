package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PredlogCeneDogadjajaDto {

    private Long dogadjajId;
    private int ukupnaKvota;
    private int ukupnoProdato;
    private int popunjenostProcenat;
    private long danaDoDogadjaja;

    @Builder.Default
    private List<PredlogCeneKarteDto> tipoviKarata = new ArrayList<>();
}
