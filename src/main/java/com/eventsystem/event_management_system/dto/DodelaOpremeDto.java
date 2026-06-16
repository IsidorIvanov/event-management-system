package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.StatusDodeleOpreme;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DodelaOpremeDto {

    private Long dodelaId;
    private Long opremaId;
    private String opremaNaziv;
    private Long dogadjajId;
    private String dogadjajNaziv;
    private Long sesijaId;
    private String sesijaNaziv;

    @NotNull(message = "Količina je obavezna")
    @Min(value = 1, message = "Količina mora biti najmanje 1")
    private Integer kolicina;

    @NotNull(message = "Datum početka je obavezan")
    private LocalDate datumOd;

    @NotNull(message = "Datum završetka je obavezan")
    private LocalDate datumDo;

    private StatusDodeleOpreme status;
    private String napomena;
    private LocalDateTime kreiranAt;
}
