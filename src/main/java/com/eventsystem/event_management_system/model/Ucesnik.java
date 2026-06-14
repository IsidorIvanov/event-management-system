package com.eventsystem.event_management_system.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

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

    /**
     * Interesovanja učesnika koja se kasnije koriste za sistem preporuka događaja.
     * Čuvaju se kao slobodne oznake (tagovi) u zasebnoj tabeli.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "ucesnik_interes",
            joinColumns = @JoinColumn(name = "korisnik_id")
    )
    @Column(name = "interes", length = 100, nullable = false)
    private Set<String> interesi = new HashSet<>();
}
