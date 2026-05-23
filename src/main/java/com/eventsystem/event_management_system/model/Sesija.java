package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.TipSesije;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sesija")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Sesija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sesija_id")
    private Long sesijaId;

    // V21: N:1 ka DOGADJAJ
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private Dogadjaj dogadjaj;

    // V22: N:1 ka SALA — kompozitni FK na (lokacija_id, naziv_sale)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "lokacija_id", referencedColumnName = "lokacija_id", nullable = false),
            @JoinColumn(name = "naziv_sale",  referencedColumnName = "naziv_sale",  nullable = false)
    })
    private Sala sala;

    @Column(nullable = false, length = 200)
    private String naziv;

    @Column(nullable = false)
    private LocalDate datum;

    @Column(name = "vreme_pocetka", nullable = false)
    private LocalTime vremePocetka;

    @Column(name = "vreme_zavrsetka", nullable = false)
    private LocalTime vremeZavrsetka;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipSesije tip;

    @Column(nullable = false)
    private Integer kapacitet;

    @Column(columnDefinition = "TEXT")
    private String opis;

    // V23: M:N sa GOVORNIK — Sesija je vlasnik veze
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sesija_govornik",
            joinColumns = @JoinColumn(name = "sesija_id"),
            inverseJoinColumns = @JoinColumn(name = "govornik_id")
    )
    private Set<Govornik> govornici = new HashSet<>();
}
