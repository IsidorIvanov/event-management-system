package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.RezultatOcene;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    private BigDecimal prihodOdKarata;
    private BigDecimal prihodOdIzlaznihFaktura;
    private BigDecimal trosakEvidentiran;
    private BigDecimal trosakHonorari;
    private BigDecimal commitovanaNabavka;
    private Integer prosecnaIskoriscenostSala;
    private Integer pokrivenostInventarProcenat;

    @Builder.Default
    private List<IzvestajFakturaRedDto> fakture = new ArrayList<>();

    @Builder.Default
    private List<IzvestajUgovorRedDto> ugovori = new ArrayList<>();

    @Builder.Default
    private List<IzvestajSalaRedDto> stavkeSala = new ArrayList<>();

    @Builder.Default
    private List<IzvestajOpremeRedDto> stavkeOpreme = new ArrayList<>();

    @Builder.Default
    private List<IzvestajNabavkaRedDto> stavkeNabavke = new ArrayList<>();

    @Builder.Default
    private List<IzvestajBudzetRedDto> stavkeBudzeta = new ArrayList<>();

    @Builder.Default
    private List<String> upozorenja = new ArrayList<>();

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

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IzvestajSalaRedDto {
        private String sesijaNaziv;
        private String nazivSale;
        private String datum;
        private Integer kapacitet;
        private Integer popunjenost;
        private Integer iskoriscenostProcenat;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IzvestajOpremeRedDto {
        private String nazivResursa;
        private Integer potrebnaKolicina;
        private Integer dodeljeno;
        private Integer dostupnoNaStanju;
        private boolean pokriveno;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IzvestajNabavkaRedDto {
        private Long nabavkaId;
        private String status;
        private String dobavljacNaziv;
        private int brojStavki;
        private BigDecimal ukupnaVrednost;
        private boolean commitovana;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IzvestajBudzetRedDto {
        private String nazivBudzeta;
        private String kategorijaNaziv;
        private BigDecimal planirano;
        private BigDecimal stvarno;
        private BigDecimal iskoriscenostProcenat;
        private String statusKontrole;
    }
}
