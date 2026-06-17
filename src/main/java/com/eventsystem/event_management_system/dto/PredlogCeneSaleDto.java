package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PredlogCeneSaleDto {

    private Long lokacijaId;
    private String nazivSale;
    private LocalDate datum;
    private BigDecimal baznaCenaPoDanu;
    private BigDecimal predlozenaCenaPoDanu;
    private int zauzetostProcenat;
    private int brojSesijaNaLokaciji;
    private String pravilo;
    private BigDecimal promenaProcenat;
}
