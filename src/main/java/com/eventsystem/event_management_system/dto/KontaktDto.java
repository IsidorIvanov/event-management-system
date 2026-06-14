package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class KontaktDto {
    private Long korisnikId;
    private String ime;
    private String prezime;
    private String tipKorisnika;   // UCESNIK | ZAPOSLENI
    private String uloga;          // null for Ucesnik, e.g. KOORDINATOR_PROGRAMA for employees
    private String poslednjaPorukaPreview;
    private LocalDateTime vremePoslednjePoruke;
    private long neprocitanihPoruka;
}

