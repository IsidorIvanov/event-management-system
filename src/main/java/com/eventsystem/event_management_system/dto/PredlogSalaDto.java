package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.TipSale;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PredlogSalaDto {

    private Long lokacijaId;
    private String nazivSale;
    private TipSale tipSale;
    private Integer kapacitet;
    private BigDecimal baznaCenaPoDanu;
    private BigDecimal predlozenaCenaPoDanu;
    private int skor;
    private boolean dostupna;
    private String obrazlozenje;

    @Builder.Default
    private List<String> upozorenja = new ArrayList<>();
}
