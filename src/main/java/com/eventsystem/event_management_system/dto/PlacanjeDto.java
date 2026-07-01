package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.MetodPlacanja;
import com.eventsystem.event_management_system.utils.enums.PlacanjeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class PlacanjeDto {
    private Long placanjeId;
    private Long fakturaId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal iznos;
    private String referentniBroj;
    private LocalDate datumPlacanja;
    private MetodPlacanja metod;
    private MetodPlacanja metodPlacanja;
    private PlacanjeStatus status;
    private String napomena;
    private String dokumentUrl;
    private LocalDateTime kreiranoAt;
    private LocalDateTime potvrdjenoAt;

    @Builder.Default
    private List<RefundacijaDto> refundacije = new ArrayList<>();
}
