package com.eventsystem.event_management_system.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "klijent")
@DiscriminatorValue("KLIJENT")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Klijent extends Korisnik {

    @Column(name = "naziv_firme", length = 200)
    private String nazivFirme;

    @Column(unique = true, length = 20)
    private String pib;

    @Column(length = 255)
    private String adresa;

    @Column(length = 100)
    private String grad;

    @Column(name = "kontakt_osoba", length = 200)
    private String kontaktOsoba;
}
