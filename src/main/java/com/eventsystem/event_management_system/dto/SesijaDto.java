package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.TipSesije;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Getter @Setter
@RequiredArgsConstructor
public class SesijaDto {

    @NotNull(message = "Događaj ID ne sme biti null")
    private final Long dogadjajId;

    @NotNull(message = "Lokacija ID ne sme biti null")
    private final Long lokacijaId;

    @NotBlank(message = "Naziv sale ne sme biti prazan")
    private final String nazivSale;

    @NotBlank(message = "Naziv sesije ne sme biti prazan")
    @Size(max = 200, message = "Naziv ne sme biti duži od 200 karaktera")
    private final String naziv;

    @NotNull(message = "Datum ne sme biti null")
    private final LocalDate datum;

    @NotNull(message = "Vreme početka ne sme biti null")
    private final LocalTime vremePocetka;

    @NotNull(message = "Vreme završetka ne sme biti null")
    private final LocalTime vremeZavrsetka;

    @NotNull(message = "Tip sesije ne sme biti null")
    private final TipSesije tip;

    @NotNull(message = "Kapacitet ne sme biti null")
    @Min(value = 1, message = "Kapacitet mora biti najmanje 1")
    private final Integer kapacitet;

    private final String opis;

    private Set<Long> govornikIds;
}
