package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StavkaAlokacijeOpremeDto {

    private String nazivPotrebe;
    private int potrebnaKolicina;
    private int vecDodeljeno;
    private int predlozenaKolicina;
    private Long opremaId;
    private String opremaNaziv;
    private String kategorija;
    private Long sesijaId;
    private String sesijaNaziv;
    private LocalDate datumOd;
    private LocalDate datumDo;
    private boolean pokriveno;
    private String obrazlozenje;
}
