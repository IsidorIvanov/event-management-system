package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.MetodPlacanja;
import com.eventsystem.event_management_system.utils.enums.PlacanjeStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlacanjeDto {
    private Long placanjeId;
    private Long fakturaId;
    private BigDecimal iznos;
    private MetodPlacanja metod;
    private PlacanjeStatus status;
    private LocalDateTime kreiranoAt;
    private LocalDateTime potvrdjenoAt;
}
