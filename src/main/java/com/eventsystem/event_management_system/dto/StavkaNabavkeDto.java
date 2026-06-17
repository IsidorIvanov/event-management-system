package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StavkaNabavkeDto {

    private Integer redniBroj;

    @NotBlank(message = "Naziv resursa je obavezan")
    @Size(max = 200)
    private String nazivResursa;

    @NotNull(message = "Količina je obavezna")
    @Min(value = 1, message = "Količina mora biti najmanje 1")
    private Integer kolicina;

    private BigDecimal jedinicnaCena;

    private BigDecimal ukupnaCena;

    private String opis;

    private Long cenovnikId;

    private Long dobavljacId;

    private String dobavljacNaziv;
}
