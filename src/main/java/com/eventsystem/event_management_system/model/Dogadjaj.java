package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.StatusDogadjaja;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@Table(name = "dogadjaj")
@NoArgsConstructor @AllArgsConstructor
public class Dogadjaj {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dogadjaj_id")
    private Long dogadjajId;

    // V4: N:1 ka LOKACIJA — obavezno (cardinalnost }o--||)
    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "lokacija_id", nullable = false)
    private Lokacija lokacija;

    @Column(nullable = false, length = 200)
    private String naziv;

    @Column(name = "datum_pocetka", nullable = false)
    private LocalDate datumPocetka;

    @Column(name = "datum_zavrsetka", nullable = false)
    private LocalDate datumZavrsetka;

    @Column(name = "maks_kapacitet", nullable = false)
    private Integer maksKapacitet;

    @Column(columnDefinition = "TEXT")
    private String opis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDogadjaja status;

    // V18: 1:N ka TIP_KARTE — slab entitet (kaskada + orphanRemoval)
    @OneToMany(mappedBy = "dogadjaj", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TipKarte> tipoviKarata = new HashSet<>();

    // V21: 1:N ka SESIJA — DDL ima ON DELETE CASCADE, prati to i u JPA
    @OneToMany(mappedBy = "dogadjaj", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Sesija> sesije = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = StatusDogadjaja.DRAFT;
        }
    }
}
