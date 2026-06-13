package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class RegistracijaDto {

    @NotNull(message = "ID događaja ne sme biti null")
    private Long dogadjajId;

    @NotBlank(message = "Naziv tipa karte ne sme biti prazan")
    private String nazivTipa;
}

