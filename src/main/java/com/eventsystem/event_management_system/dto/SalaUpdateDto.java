package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.TipSale;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaUpdateDto {

    @NotNull(message = "Tip sale ne sme biti null")
    private TipSale tipSale;

    @NotNull(message = "Kapacitet ne sme biti null")
    @Min(value = 1, message = "Kapacitet mora biti najmanje 1")
    private Integer kapacitet;

    @NotNull(message = "Bazna cena po danu ne sme biti null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Cena mora biti veca od 0")
    private BigDecimal baznaCenaPoDanu;
}
