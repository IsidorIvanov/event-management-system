package com.eventsystem.event_management_system.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StavkaFaktureDto {
    private Long stavkaFaktureId;
    private Long fakturaId;
    private Integer redniBroj;
    private String naziv;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal kolicina;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal jedinicnaCena;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal ukupnaCena;
    private String napomena;
    private Long budzetId;
    private Long kategorijaId;
    private Long trosakId;
}
