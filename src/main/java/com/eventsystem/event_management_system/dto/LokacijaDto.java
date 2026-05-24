package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LokacijaDto {

    private Long lokacijaId;

    @NotBlank(message = "Naziv ne sme biti prazan")
    @Size(max = 200, message = "Naziv ne sme biti duži od 200 karaktera")
    private String naziv;

    @NotBlank(message = "Adresa ne sme biti prazna")
    @Size(max = 200)
    private String adresa;

    @NotBlank(message = "Grad ne sme biti prazan")
    @Size(max = 100)
    private String grad;

    @NotBlank(message = "Država ne sme biti prazna")
    private String drzava;

    @Builder.Default
    private List<SalaDto> sale = new ArrayList<>();
}
