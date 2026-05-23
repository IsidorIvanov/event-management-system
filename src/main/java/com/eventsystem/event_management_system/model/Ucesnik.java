package com.eventsystem.event_management_system.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "ucesnik")
@DiscriminatorValue("UCESNIK")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Ucesnik extends Korisnik {

    @Column(length = 200)
    private String kompanija;

    @Column(length = 100)
    private String pozicija;

    @Column(name = "datum_rodjenja")
    private LocalDate datumRodjenja;
}
