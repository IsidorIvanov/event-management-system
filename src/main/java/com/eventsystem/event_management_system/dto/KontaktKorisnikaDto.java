package com.eventsystem.event_management_system.dto;

import lombok.*;

/** Kontakt podaci korisnika (organizatora ili učesnika) vezanog za događaj. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class KontaktKorisnikaDto {
    private Long korisnikId;
    private String ime;
    private String prezime;
    private String email;
    private String telefon;
    private String uloga;       // popunjeno za organizatore (npr. KOORDINATOR_PROGRAMA)
    private String kompanija;   // popunjeno za učesnike
    private String pozicija;
}
