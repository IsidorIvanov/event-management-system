package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import com.eventsystem.event_management_system.utils.enums.TipSesije;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SesijaDetaljDto {

    private Long sesijaId;
    private String naziv;
    private LocalDate datum;
    private LocalTime vremePocetka;
    private LocalTime vremeZavrsetka;
    private TipSesije tip;
    private Integer kapacitet;
    private String opis;

    private String nazivSale;
    private Long lokacijaId;
    private String lokacijaNaziv;

    private Long dogadjajId;
    private String dogadjajNaziv;
    private StatusDogadjaja dogadjajStatus;
    private LocalDate dogadjajDatumOd;
    private LocalDate dogadjajDatumDo;
    private String dogadjajOpis;

    @Builder.Default
    private List<GovornikPregledDto> govornici = new ArrayList<>();

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GovornikPregledDto {
        private String ime;
        private String prezime;
        private String kompanija;
        private String pozicija;
    }
}
