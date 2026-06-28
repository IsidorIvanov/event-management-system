package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.RezultatOcene;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IzvestajPodaciDto {

    private String naslov;
    private String podnaslov;
    private String izvorOznaka;

    private BigDecimal ukupanPrihod;
    private BigDecimal ukupanTrosak;
    private BigDecimal neto;
    private BigDecimal marza;
    private RezultatOcene rezultatOcene;

    private BigDecimal sumaFakturisano;
    private BigDecimal sumaNaplaceno;
    private BigDecimal otvorenoPotrazivanje;
    private BigDecimal sumaPlaceno;

    private Long brojDogadjaja;

    private List<IzvestajFakturaRedDto> fakture;
    private List<IzvestajUgovorRedDto> ugovori;

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IzvestajFakturaRedDto {
        private String brojFakture;
        private String datum;
        private BigDecimal ukupanIznos;
        private BigDecimal placeniIznos;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IzvestajUgovorRedDto {
        private String brojUgovora;
        private String predmet;
        private String status;
        private BigDecimal vrednost;
        private String vaziDo;
    }
}
