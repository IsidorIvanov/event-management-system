package com.eventsystem.event_management_system.dto;

import lombok.*;

@Getter @Setter
@RequiredArgsConstructor
public class LokacijaDto {

    private final String naziv;

    private final String adresa;

    private final String grad;

    private final String drzava;
}
