package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.StatusKorisnika;
import com.eventsystem.event_management_system.utils.TipKorisnika;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "korisnik")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "tip_korisnika", discriminatorType = DiscriminatorType.STRING)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public abstract class Korisnik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "korisnik_id")
    private Long korisnikId;

    @Column(nullable = false, length = 100)
    private String ime;

    @Column(nullable = false, length = 100)
    private String prezime;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "lozinka_hash", nullable = false)
    private String lozinkaHash;

    @Column(length = 30)
    private String telefon;

    @Column(name = "datum_registracije")
    private LocalDate datumRegistracije;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusKorisnika status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tip_korisnika", insertable = false, updatable = false, length = 20)
    private TipKorisnika tipKorisnika;

    @PrePersist
    protected void onCreate() {
        if (datumRegistracije == null) {
            datumRegistracije = LocalDate.now();
        }
        if (status == null) {
            status = StatusKorisnika.AKTIVAN;
        }
    }
}
