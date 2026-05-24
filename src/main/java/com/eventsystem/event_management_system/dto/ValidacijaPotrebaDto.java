package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidacijaPotrebaDto {

    private Long dogadjajId;
    private String dogadjajNaziv;
    private int ukupnoPotreba;
    private int pokrivenePotrebe;
    private boolean validno;

    @Builder.Default
    private List<DetektovanaPotrebaDto> detektovanePotrebe = new ArrayList<>();

    @Builder.Default
    private List<String> upozorenja = new ArrayList<>();

    @Builder.Default
    private List<String> nepokriveneStavke = new ArrayList<>();
}
