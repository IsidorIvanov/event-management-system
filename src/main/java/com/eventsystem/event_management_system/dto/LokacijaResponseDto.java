package com.eventsystem.event_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LokacijaResponseDto {

    private Long lokacijaId;

    private String naziv;

    private String adresa;

    private String grad;

    private String drzava;
}
