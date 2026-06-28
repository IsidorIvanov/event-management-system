package com.eventsystem.event_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Agregirani podaci za "Reports & Analytics" tab na stranici detalja događaja.
 * Sadrži vremensku seriju stope prisustva (registracije kroz vreme) i
 * po-sesijski engagement (popunjenost sesija).
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DogadjajAnalitikaDto {

    private Long dogadjajId;
    private String dogadjajNaziv;
    private Integer maksKapacitet;

    /** Ukupno potvrđenih registracija na događaju. */
    private long ukupnoRegistracija;

    /** Stopa prisustva = potvrđene registracije / maks. kapacitet (%). */
    private double stopaPrisustva;

    /** Prosečna popunjenost sesija (%). */
    private double prosecniEngagement;

    /** Linijski grafikon: kumulativna stopa prisustva kroz vreme. */
    private List<TackaDto> stopaPrisustvaSerija;

    /** Stubičasti grafikon: engagement (popunjenost %) po sesiji. */
    private List<TackaDto> engagementPoSesiji;

    @Builder
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class TackaDto {
        private String oznaka;
        private double vrednost;
    }
}
