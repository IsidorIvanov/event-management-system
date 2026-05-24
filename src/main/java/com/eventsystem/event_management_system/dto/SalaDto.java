package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.TipSale;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@RequiredArgsConstructor
@Builder
public class SalaDto {

    @NotNull(message = "Lokacija ID ne sme biti null")
    private final Long lokacijaId;

    @NotBlank(message = "Naziv sale ne sme biti prazan")
    @Size(max = 100, message = "Naziv sale ne sme biti duzi od 100 karaktera")
    private final String nazivSale;

    @NotNull(message = "Tip sale ne sme biti null")
    private final TipSale tipSale;

    @NotNull(message = "Kapacitet ne sme biti null")
    @Min(value = 1, message = "Kapacitet mora biti najmanje 1")
    private final Integer kapacitet;

    @NotNull(message = "Bazna cena po danu ne sme biti null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Cena mora biti veca od 0")
    private final BigDecimal baznaCenaPoDanu;
}
