package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.RezultatOcene;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DogadjajResursiTroskoviIzvestajDto {

    private Long dogadjajId;
    private String dogadjajNaziv;
    private LocalDateTime generisanoAt;

    private RezimeDto rezime;
    private ResursiDto resursi;
    private TroskoviDto troskovi;

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RezimeDto {
        private Integer prosecnaIskoriscenostSala;
        private Integer pokrivenostInventarProcenat;
        private BigDecimal ukupanPrihod;
        private BigDecimal ukupanTrosak;
        private BigDecimal neto;
        private RezultatOcene rezultatOcene;
        private int brojUpozorenja;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResursiDto {
        private Integer prosecnaIskoriscenostSala;
        private Integer pokrivenostInventarProcenat;
        private boolean svePokrivenoInventarom;

        @Builder.Default
        private List<SalaStavkaDto> stavkeSala = new ArrayList<>();

        @Builder.Default
        private List<OpremeStavkaDto> stavkeOpreme = new ArrayList<>();

        @Builder.Default
        private List<DodelaOpremeDto> aktivneDodele = new ArrayList<>();

        @Builder.Default
        private List<NabavkaStavkaDto> stavkeNabavke = new ArrayList<>();

        @Builder.Default
        private List<String> upozorenja = new ArrayList<>();
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaStavkaDto {
        private Long sesijaId;
        private String sesijaNaziv;
        private String nazivSale;
        private String lokacijaNaziv;
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
    public static class OpremeStavkaDto {
        private String nazivResursa;
        private Integer potrebnaKolicina;
        private Integer dodeljenoDogadjaju;
        private Integer dostupnoNaStanju;
        private boolean pokriveno;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NabavkaStavkaDto {
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
    public static class TroskoviDto {
        private String izvorOznaka;
        private BigDecimal ukupanPrihod;
        private BigDecimal ukupanTrosak;
        private BigDecimal neto;
        private BigDecimal marza;
        private RezultatOcene rezultatOcene;

        private BigDecimal prihodOdKarata;
        private BigDecimal prihodOdIzlaznihFaktura;
        private BigDecimal trosakEvidentiran;
        private BigDecimal trosakHonorari;
        private BigDecimal commitovanaNabavka;

        @Builder.Default
        private List<BudzetStavkaDto> iskoriscenostBudzeta = new ArrayList<>();
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudzetStavkaDto {
        private Long budzetId;
        private String nazivBudzeta;
        private String kategorijaNaziv;
        private BigDecimal planirano;
        private BigDecimal stvarno;
        private BigDecimal iskoriscenostProcenat;
        private String statusKontrole;
    }
}
