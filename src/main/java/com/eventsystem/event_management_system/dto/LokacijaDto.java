package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
@RequiredArgsConstructor
public class LokacijaDto {

    @NotBlank(message = "Naziv ne sme biti prazan")
    @Size(max = 200, message = "Naziv ne sme biti duži od 200 karaktera")
    private final String naziv;

    @NotBlank(message = "Adresa ne sme biti prazna")
    @Size(max = 200)
    private final String adresa;

    @NotBlank(message = "Grad ne sme biti prazan")
    @Size(max = 100)
    private final String grad;

    @NotBlank(message = "Država ne sme biti prazna")
    private final String drzava;
}
