package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.StatusKontrole;
import com.eventsystem.event_management_system.utils.enums.AlertLevel;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
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
public class StavkaBudzetaDto {

    private Long budzetId;

    @NotNull(message = "ID kategorije je obavezan")
    private Long kategorijaId;

    private String kategorijaNaziv;

    private String kategorijaOpis;

    @NotNull(message = "Planirani iznos stavke je obavezan")
    @DecimalMin(value = "0.00", message = "Planirani iznos ne može biti negativan")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal planiraniIznos;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal stvarniIznos;

    @DecimalMin(value = "0.0000", message = "Prag upozorenja ne može biti negativan")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal pragUpozorenja;

    @DecimalMin(value = "0.0000", message = "Kritični prag ne može biti negativan")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal pragKriticnog;

    private StatusKontrole statusKontrole;

    private AlertLevel alertLevel;

    private BigDecimal iskoriscenost;

    private String alertPoruka;

    @Size(max = 500, message = "Komentar može imati najviše 500 karaktera")
    private String komentar;
}
