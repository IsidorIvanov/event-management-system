package com.eventsystem.event_management_system.dto;

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

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUgovorRequest {

    @NotBlank(message = "Broj ugovora je obavezan")
    @Size(max = 64, message = "Broj ugovora može imati najviše 64 karaktera")
    private String brojUgovora;

    @NotNull(message = "ID dobavljača je obavezan")
    private Long dobavljacId;

    @NotNull(message = "ID događaja je obavezan")
    private Long dogadjajId;

    @NotNull(message = "Datum potpisivanja je obavezan")
    private LocalDate datumPotpisivanja;

    @NotNull(message = "Datum isteka je obavezan")
    private LocalDate vaziDo;

    @NotNull(message = "Vrednost ugovora je obavezna")
    @DecimalMin(value = "0.00", message = "Vrednost ugovora ne može biti negativna")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal vrednost;

    @NotBlank(message = "Predmet ugovora je obavezan")
    @Size(max = 500, message = "Predmet ugovora može imati najviše 500 karaktera")
    private String predmet;

    private String usloviPlacanja;
}
