package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerisiPorudzbenicuRequestDto {

    @NotNull(message = "ID nabavke je obavezan")
    private Long nabavkaId;

    private LocalDate rokIsporuke;
    private String napomena;
}
