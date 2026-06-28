package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.AnalizaStatus;
import com.eventsystem.event_management_system.utils.enums.RezultatOcene;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalizaProfitabilnostiDto {

    private Long analizaId;
    private Long dogadjajId;
    private String dogadjajNaziv;
    private Long kreiraoId;
    private String kreiraoImePrezime;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal ukupanPrihod;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal ukupanTrosak;

    /** Obaveze iz commitovanih nabavki — informativno, van ocene; samo za DRAFT analizu. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal commitovaniTrosak;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal prihodRegistracije;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal prihodIzlazneFakture;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal trosakEvidentiran;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal trosakHonorari;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal neto;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal marza;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal roi;

    private LocalDate datumAnalize;
    private LocalDateTime finalizovanoAt;
    private RezultatOcene rezultatOcene;
    private AnalizaStatus status;
    private String napomene;
}
