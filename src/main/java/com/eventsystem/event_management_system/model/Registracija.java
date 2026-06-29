package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.StatusKarte;
import com.eventsystem.event_management_system.utils.enums.StatusRegistracije;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registracija")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Registracija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registracija_id")
    private Long registracijaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "korisnik_id", nullable = false)
    private Ucesnik ucesnik;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "dogadjaj_id", referencedColumnName = "dogadjaj_id", nullable = false),
        @JoinColumn(name = "naziv_tipa",  referencedColumnName = "naziv_tipa",  nullable = false)
    })
    private TipKarte tipKarte;

    @Column(name = "datum_registracije")
    private LocalDate datumRegistracije;

    /** Tačan trenutak registracije (sa satom) — koristi se za satnu agregaciju izveštaja. */
    @Column(name = "vreme_registracije")
    private LocalDateTime vremeRegistracije;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRegistracije status;

    @Column(name = "broj_karte", length = 100, unique = true)
    private String brojKarte;

    @Column(name = "qr_kod", length = 255)
    private String qrKod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_karte", nullable = false, length = 20)
    private StatusKarte statusKarte;

    @PrePersist
    protected void onCreate() {
        if (vremeRegistracije == null) vremeRegistracije = LocalDateTime.now();
        if (datumRegistracije == null) datumRegistracije = vremeRegistracije.toLocalDate();
        if (status == null) status = StatusRegistracije.POTVRDJENA;
        if (statusKarte == null) statusKarte = StatusKarte.VALIDNA;
    }
}

