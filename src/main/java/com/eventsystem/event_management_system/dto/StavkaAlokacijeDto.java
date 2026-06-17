package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StavkaAlokacijeDto {

    private String nazivResursa;
    private Integer kolicina;
    private Long cenovnikId;
    private Long dobavljacId;
    private String dobavljacNaziv;
    private BigDecimal jedinicnaCena;
    private BigDecimal ukupnaCena;
    private boolean pokriveno;
}
