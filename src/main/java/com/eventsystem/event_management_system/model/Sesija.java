package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.TipSesije;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@Table(name = "sesija")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Sesija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sesija_id")
    private Long sesijaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private Dogadjaj dogadjaj;

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

    /** Da li je već poslat podsetnik „sesija počinje uskoro" (P2) — sprečava ponovno slanje. */
    @Column(name = "podsetnik_poslat", columnDefinition = "boolean default false")
    private boolean podsetnikPoslat;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sesija_govornik",
            joinColumns = @JoinColumn(name = "sesija_id"),
            inverseJoinColumns = @JoinColumn(name = "govornik_id")
    )
    @Builder.Default
    private Set<Govornik> govornici = new HashSet<>();
}
