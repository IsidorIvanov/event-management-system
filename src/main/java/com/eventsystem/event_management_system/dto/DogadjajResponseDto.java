package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class DogadjajResponseDto {

    private Long dogadjajId;

    private String naziv;

    private String datumPocetka;

    private String datumZavrsetka;

    private Integer maksKapacitet;

    private String opis;

    private StatusDogadjaja status;

    private String lokacijaNaziv;

    private String lokacijaGrad;

    private String lokacijaDrzava;

    private String lokacijaAdresa;

    private Set<String> tagovi;
}
