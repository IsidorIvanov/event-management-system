package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.StatusPorudzbenice;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PorudzbenicaDto {

    private Long porudzbenicaId;
    private Long nabavkaId;
    private Long dogadjajId;
    private String dogadjajNaziv;
    private Long dobavljacId;
    private String dobavljacNaziv;
    private String brojPorudzbenice;
    private StatusPorudzbenice status;
    private BigDecimal ukupnaCena;
    private LocalDateTime kreiranAt;
    private LocalDateTime poslataAt;
    private LocalDateTime potvrdjenaAt;
    private LocalDate rokIsporuke;
    private LocalDateTime isporucenaAt;
    private String napomena;

    @Builder.Default
    private List<StavkaPorudzbeniceDto> stavke = new ArrayList<>();
}
