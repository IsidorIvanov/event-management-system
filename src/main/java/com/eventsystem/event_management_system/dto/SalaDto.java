package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.TipSale;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaDto {

    @NotNull(message = "Lokacija ID ne sme biti null")
    private Long lokacijaId;

    @NotBlank(message = "Naziv sale ne sme biti prazan")
    @Size(max = 100, message = "Naziv sale ne sme biti duzi od 100 karaktera")
    private String nazivSale;

    @NotNull(message = "Tip sale ne sme biti null")
    private TipSale tipSale;

    @NotNull(message = "Kapacitet ne sme biti null")
    @Min(value = 1, message = "Kapacitet mora biti najmanje 1")
    private Integer kapacitet;

    @NotNull(message = "Bazna cena po danu ne sme biti null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Cena mora biti veca od 0")
    private BigDecimal baznaCenaPoDanu;
}
