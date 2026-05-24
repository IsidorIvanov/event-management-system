package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CenovnikDto {

    private Long cenovnikId;

    @NotNull(message = "ID dobavljača je obavezan")
    private Long dobavljacId;

    private String dobavljacNaziv;

    @NotBlank(message = "Naziv resursa je obavezan")
    @Size(max = 200)
    private String nazivResursa;

    private String opis;

    @NotBlank(message = "Jedinica mere je obavezna")
    @Size(max = 30)
    private String jedinicaMere;

    @NotNull(message = "Cena je obavezna")
    @DecimalMin(value = "0.01", message = "Cena mora biti pozitivna")
    private BigDecimal cenaJedinicna;

    private Boolean dostupnost;
}
