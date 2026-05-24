package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.StatusDobavljaca;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DobavljacDto {

    private Long dobavljacId;

    @NotBlank(message = "Naziv dobavljača ne sme biti prazan")
    @Size(max = 200)
    private String naziv;

    @NotBlank(message = "Grad ne sme biti prazan")
    @Size(max = 100)
    private String grad;

    @NotBlank(message = "Kontakt email je obavezan")
    @Email(message = "Email mora biti validan")
    @Size(max = 150)
    private String kontaktEmail;

    @Size(max = 30)
    private String telefon;

    @NotBlank(message = "PIB je obavezan")
    @Size(max = 20)
    private String pib;

    private StatusDobavljaca status;

    @DecimalMin(value = "0.0", message = "Rejting mora biti >= 0")
    @DecimalMax(value = "5.0", message = "Rejting mora biti <= 5")
    private BigDecimal rejting;

    private String kriterijumi;
}
