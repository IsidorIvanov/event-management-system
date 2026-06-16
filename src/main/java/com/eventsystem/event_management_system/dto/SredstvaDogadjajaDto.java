package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SredstvaDogadjajaDto {

    private Long dogadjajId;
    private String dogadjajNaziv;

    @Builder.Default
    private List<SalaSredstvoDto> sale = new ArrayList<>();

    @Builder.Default
    private List<NabavkaSredstvoDto> nabavke = new ArrayList<>();

    @Builder.Default
    private List<DodelaOpremeDto> dodeleOpreme = new ArrayList<>();

    @Builder.Default
    private List<String> upozorenja = new ArrayList<>();

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaSredstvoDto {
        private String nazivSale;
        private String lokacijaNaziv;
        private Long sesijaId;
        private String sesijaNaziv;
        private String datum;
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NabavkaSredstvoDto {
        private Long nabavkaId;
        private String status;
        private String dobavljacNaziv;
        private String stavkeOpis;
    }
}
