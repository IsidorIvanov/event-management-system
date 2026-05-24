package com.eventsystem.event_management_system.dto;

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
public class SalaDostupnostDto {

    private Long lokacijaId;
    private String nazivSale;
    private LocalDate datumOd;
    private LocalDate datumDo;
    private int satPocetka;
    private int satZavrsetka;

    @Builder.Default
    private List<DanDostupnostiDto> dani = new ArrayList<>();

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DanDostupnostiDto {
        private LocalDate datum;
        @Builder.Default
        private List<VremenskiSlotDto> slotovi = new ArrayList<>();
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VremenskiSlotDto {
        private LocalTime vremeOd;
        private LocalTime vremeDo;
        private boolean dostupno;
        private String nazivSesije;
        private Long sesijaId;
    }
}
