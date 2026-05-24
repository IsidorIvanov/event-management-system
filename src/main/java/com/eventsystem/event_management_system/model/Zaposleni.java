package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.UlogaZaposlenog;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "zaposleni")
@DiscriminatorValue("ZAPOSLENI")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Zaposleni extends Korisnik {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UlogaZaposlenog uloga;

    @Column(length = 100)
    private String pozicija;

    @Column(name = "datum_zaposlenja")
    private LocalDate datumZaposlenja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nadredjeni_id")
    private Zaposleni nadredjeni;
}
