package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.TipSesije;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RasporedSesijaDto {

    private Long sesijaId;
    private String naziv;
    private LocalDate datum;
    private LocalTime vremePocetka;
    private LocalTime vremeZavrsetka;
    private TipSesije tip;
    private Integer kapacitet;
    private String opis;
    private String nazivSale;

    private Long dogadjajId;
    private String dogadjajNaziv;

    private Long lokacijaId;
    private String lokacijaNaziv;
    private String lokacijaGrad;

    private List<GovornikImeDto> govornici;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GovornikImeDto {
        private Long govornikId;
        private String ime;
        private String prezime;
        private String kompanija;
        private String pozicija;
    }
}

