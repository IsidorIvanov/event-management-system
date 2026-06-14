package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.TipScenarija;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateScenarioPrognozeRequest {

    @NotBlank(message = "Naziv scenarija je obavezan")
    @Size(max = 200, message = "Naziv scenarija može imati najviše 200 karaktera")
    private String nazivScenarija;

    @NotNull(message = "Tip scenarija je obavezan")
    private TipScenarija tipScenarija;

    @NotNull(message = "Projektovani prihod je obavezan")
    @DecimalMin(value = "0.00", message = "Projektovani prihod ne može biti negativan")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal projektovaniPrihod;

    @NotNull(message = "Projektovani trošak je obavezan")
    @DecimalMin(value = "0.00", message = "Projektovani trošak ne može biti negativan")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal projektovaniTrosak;

    private String pretpostavke;
}
