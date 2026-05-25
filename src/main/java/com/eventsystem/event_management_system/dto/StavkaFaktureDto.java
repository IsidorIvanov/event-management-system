package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StavkaFaktureDto {
    private Long stavkaFaktureId;
    private Integer redniBroj;
    private String naziv;
    private Integer kolicina;
    private BigDecimal jedinicnaCena;
    private BigDecimal ukupnaCena;
    private String napomena;
    private Long budzetId;
    private Long kategorijaId;
}
