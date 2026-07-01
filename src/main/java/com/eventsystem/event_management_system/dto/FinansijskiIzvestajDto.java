package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.FormatIzvestaja;
import com.eventsystem.event_management_system.utils.enums.IzvestajStatus;
import com.eventsystem.event_management_system.utils.enums.TipIzvestaja;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinansijskiIzvestajDto {

    private Long izvestajId;
    private TipIzvestaja tip;
    private FormatIzvestaja format;
    private IzvestajStatus status;
    private Long dogadjajId;
    private Long klijentId;
    private Long dobavljacId;
    private LocalDate periodOd;
    private LocalDate periodDo;
    private String fileUrl;
    private String greskaPoruka;
    private Long kreiraoId;
    private String kreiraoImePrezime;
    private LocalDateTime kreiranAt;
    private LocalDateTime generisanoAt;
}
