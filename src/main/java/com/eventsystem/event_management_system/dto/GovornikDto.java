package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GovornikDto {

    @NotBlank(message = "Ime ne sme biti prazno")
    @Size(max = 100, message = "Ime ne sme biti duze od 100 karaktera")
    private String ime;

    @NotBlank(message = "Prezime ne sme biti prazno")
    @Size(max = 100, message = "Prezime ne sme biti duze od 100 karaktera")
    private String prezime;

    @Size(max = 200, message = "Naziv kompanije ne sme biti duzi od 200 karaktera")
    private String kompanija;

    @Size(max = 100, message = "Pozicija ne sme biti duza od 100 karaktera")
    private String pozicija;

    private String biografija;

    @Email(message = "Email format nije validan")
    @Size(max = 150)
    private String email;

    @NotNull(message = "Honorar ne sme biti null")
    @DecimalMin(value = "0.0", message = "Honorar ne sme biti negativan")
    private BigDecimal honorar;
}
