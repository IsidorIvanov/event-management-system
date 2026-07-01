package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.RefundacijaStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundacijaDto {
    private Long refundacijaId;
    private Long placanjeId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal iznos;
    private RefundacijaStatus status;
    private Long kreiraoId;
    private Long odobrioId;
    private LocalDateTime izvrsenoAt;
    private LocalDateTime kreiranoAt;
    private String razlog;
}
