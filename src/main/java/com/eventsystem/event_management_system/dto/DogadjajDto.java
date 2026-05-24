package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
@RequiredArgsConstructor
public class DogadjajDto {

    @NotNull(message = "Lokacija ID ne sme biti null")
    private final Long lokacijaId;

    @NotBlank(message = "Naziv ne sme biti prazan")
    @Size(max = 200, message = "Naziv ne sme biti duži od 200 karaktera")
    private final String naziv;

    @NotBlank(message = "Datum početka ne sme biti prazan")
    private final String datumPocetka;

    @NotBlank(message = "Datum završetka ne sme biti prazan")
    private final String datumZavrsetka;

    @NotNull(message = "Maksimalni kapacitet ne sme biti null")
    @Min(value = 1, message = "Maksimalni kapacitet mora biti pozitivan broj")
    private final Integer maksKapacitet;

    private final String opis;
}
