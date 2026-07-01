package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.StatusOpreme;
import jakarta.validation.constraints.*;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpremaDto {

    private Long opremaId;

    @NotBlank(message = "Naziv opreme je obavezan")
    @Size(max = 200)
    private String naziv;

    @NotBlank(message = "Kategorija je obavezna")
    @Size(max = 80)
    private String kategorija;

    private Long cenovnikId;

    @Min(value = 0, message = "Ukupna količina mora biti >= 0")
    private Integer kolicinaUkupno;

    @Min(value = 0, message = "Dostupna količina mora biti >= 0")
    private Integer kolicinaDostupno;

    private Integer kolicinaRezervisano;
    private Integer kolicinaNaDogadjaju;
    private Integer kolicinaUServisu;

    private StatusOpreme status;

    @Size(max = 100)
    private String lokacijaSkladista;

    @Size(max = 80)
    private String serijskiBroj;

    private String napomena;
}
