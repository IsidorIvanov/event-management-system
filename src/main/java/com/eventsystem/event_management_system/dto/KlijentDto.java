package com.eventsystem.event_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KlijentDto {
    private Long klijentId;
    private Long korisnikId;
    private String ime;
    private String prezime;
    private String email;
    private String nazivFirme;
    private String pib;
    private String kontaktOsoba;
}
