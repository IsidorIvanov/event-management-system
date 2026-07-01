package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.BudzetStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudzetDto {

    private Long budzetId;

    @NotNull(message = "ID događaja je obavezan")
    private Long dogadjajId;

    private String dogadjajNaziv;

    private String dogadjajStatus;

    private Long kreiraoId;

    private String kreiraoIme;

    @NotBlank(message = "Naziv budžeta je obavezan")
    @Size(max = 255, message = "Naziv budžeta može imati najviše 255 karaktera")
    private String nazivBudzeta;

    @NotNull(message = "Planirani iznos je obavezan")
    @DecimalMin(value = "0.00", message = "Planirani iznos ne može biti negativan")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal planiraniIznos;

    @DecimalMin(value = "0.00", message = "Odobreni iznos ne može biti negativan")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal odobreniIznos;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal ukupnoPlaniranoStavke;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal ukupnoStvarnoStavke;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal preostaloZaPlaniranje;

    private BudzetStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime zatvorenAt;

    @Valid
    @Builder.Default
    private List<StavkaBudzetaDto> stavke = new ArrayList<>();
}
