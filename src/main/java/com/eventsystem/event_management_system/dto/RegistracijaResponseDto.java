package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.StatusKarte;
import com.eventsystem.event_management_system.utils.enums.StatusRegistracije;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RegistracijaResponseDto {

    private Long registracijaId;
    private Long dogadjajId;
    private String dogadjajNaziv;
    private String dogadjajDatumPocetka;
    private String dogadjajDatumZavrsetka;
    private String dogadjajStatus;
    private String lokacijaGrad;
    private String lokacijaDrzava;
    private String nazivTipa;
    private String datumRegistracije;
    private StatusRegistracije status;
    private String brojKarte;
    private StatusKarte statusKarte;
}

