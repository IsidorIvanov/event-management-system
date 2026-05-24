package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.VrstaKarte;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TipKarteDto {

    @NotNull(message = "ID događaja ne sme biti null")
    private Long dogadjajId;

    @NotBlank(message = "Naziv tipa ne sme biti prazan")
    @Size(max = 100, message = "Naziv tipa ne sme biti duži od 100 karaktera")
    private String nazivTipa;

    @NotNull(message = "Vrsta karte ne sme biti null")
    private VrstaKarte vrsta;

    @NotNull(message = "Cena ne sme biti null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Cena ne sme biti negativna")
    private BigDecimal cena;

    @NotNull(message = "Kvota ne sme biti null")
    @Min(value = 1, message = "Kvota mora biti najmanje 1")
    private Integer kvota;

    @Size(max = 500, message = "Opis ne sme biti duži od 500 karaktera")
    private String opis;
}
