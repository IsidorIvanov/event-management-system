package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PotrebnaStavkaDto {

    @NotBlank(message = "Naziv resursa je obavezan")
    @Size(max = 200)
    private String nazivResursa;

    @NotNull(message = "Količina je obavezna")
    @Min(value = 1, message = "Količina mora biti najmanje 1")
    private Integer kolicina;
}
