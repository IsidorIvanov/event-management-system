package com.eventsystem.event_management_system.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @Setter
@RequiredArgsConstructor
public class DogadjajDto {

    private final Long lokacijaId;

    private final String naziv;

    private final String datumPocetka;

    private final String datumZavrsetka;

    private final Integer maksKapacitet;

    private final String opis;
}
