package com.eventsystem.event_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Zauzeti termin na nekoj lokaciji — koristi se da forma za kreiranje/izmenu
 * događaja prikaže koji su datumi već zauzeti i da spreči preklapanje.
 */
@Getter
@AllArgsConstructor
public class ZauzetiTerminDto {

    private Long dogadjajId;

    private String naziv;

    private String datumPocetka;

    private String datumZavrsetka;
}
