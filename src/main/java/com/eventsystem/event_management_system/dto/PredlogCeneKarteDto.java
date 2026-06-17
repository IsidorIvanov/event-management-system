package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PredlogCeneKarteDto {

    private Long dogadjajId;
    private String nazivTipa;
    private BigDecimal trenutnaCena;
    private BigDecimal predlozenaCena;
    private Integer kvota;
    private int prodato;
    private int popunjenostProcenat;
    private long danaDoDogadjaja;
    private String pravilo;
    private BigDecimal promenaProcenat;
}
