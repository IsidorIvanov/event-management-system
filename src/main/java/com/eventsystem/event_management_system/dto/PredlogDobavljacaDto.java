package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.KriterijumSelekcijeDobavljaca;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PredlogDobavljacaDto {

    private Long dobavljacId;
    private String dobavljacNaziv;
    private String kontaktEmail;
    private BigDecimal rejting;
    private BigDecimal ukupnaProcenjenaCena;
    private KriterijumSelekcijeDobavljaca kriterijum;
    private boolean pokrivaSveStavke;

    @Builder.Default
    private List<StavkaPredlogDto> mapiranjeStavki = new ArrayList<>();

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StavkaPredlogDto {
        private String nazivResursa;
        private Integer kolicina;
        private Long cenovnikId;
        private BigDecimal jedinicnaCena;
        private BigDecimal ukupnaCena;
    }
}
