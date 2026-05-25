package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
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
public class FakturaDto {
    private Long fakturaId;
    private String brojFakture;
    private TipFakture tip;
    private FakturaStatus status;
    private BigDecimal ukupnaIznos;
    private BigDecimal placeniIznos;
    private LocalDate datumIzdavanja;
    private Long dogadjajId;
    private Long dobavljacId;
    private Long kreiraoId;
    private LocalDate rokPlacanja;
    private String napomena;
    private LocalDateTime kreiranAt;

    @Builder.Default
    private List<StavkaFaktureDto> stavke = new ArrayList<>();
}
