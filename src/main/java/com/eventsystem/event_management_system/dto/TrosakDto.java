package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.TipTroska;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrosakDto {

    private Long trosakId;

    @NotNull(message = "ID budžeta je obavezan")
    private Long budzetId;

    @NotNull(message = "ID kategorije je obavezan")
    private Long kategorijaId;

    @NotNull(message = "ID događaja je obavezan")
    private Long dogadjajId;

    private Long dobavljacId;

    private Long evidentiraoId;

    private Long fakturaId;

    private Long stavkaFaktureId;

    @NotBlank(message = "Opis troška je obavezan")
    @Size(max = 500, message = "Opis troška može imati najviše 500 karaktera")
    private String opis;

    @NotNull(message = "Iznos troška je obavezan")
    @DecimalMin(value = "0.00", message = "Iznos troška ne može biti negativan")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal iznos;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal refundiraniIznos;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal netoIznos;

    @NotNull(message = "Datum troška je obavezan")
    private LocalDate datumTroska;

    @NotNull(message = "Tip troška je obavezan")
    private TipTroska tip;

    private LocalDateTime kreiranAt;
}
