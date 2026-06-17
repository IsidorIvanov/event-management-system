package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrimeniCenuRequestDto {

    @NotNull
    private BigDecimal cena;
}
