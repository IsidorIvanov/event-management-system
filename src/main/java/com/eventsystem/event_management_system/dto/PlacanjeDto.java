package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.MetodPlacanja;
import com.eventsystem.event_management_system.utils.enums.PlacanjeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
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
    private MetodPlacanja metod;
    private PlacanjeStatus status;
    private String napomena;
    private LocalDateTime kreiranoAt;
    private LocalDateTime potvrdjenoAt;

    @Builder.Default
    private List<RefundacijaDto> refundacije = new ArrayList<>();
}
