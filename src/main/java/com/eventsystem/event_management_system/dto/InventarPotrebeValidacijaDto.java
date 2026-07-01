package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventarPotrebeValidacijaDto {

    private Long dogadjajId;
    private boolean svePokriveno;
    private int tacnostProcenat;

    @Builder.Default
    private List<StavkaPokrivenostDto> stavke = new ArrayList<>();

    @Builder.Default
    private List<String> upozorenja = new ArrayList<>();

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StavkaPokrivenostDto {
        private String nazivResursa;
        private Integer potrebnaKolicina;
        private Integer dostupnoNaStanju;
        private boolean pokriveno;
    }
}
