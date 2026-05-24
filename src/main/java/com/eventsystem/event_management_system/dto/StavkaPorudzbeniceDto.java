package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StavkaPorudzbeniceDto {

    private Integer redniBroj;
    private String nazivResursa;
    private Integer kolicina;
    private BigDecimal jedinicnaCena;
    private BigDecimal ukupnaCena;
    private String razlogPotrebe;
}
