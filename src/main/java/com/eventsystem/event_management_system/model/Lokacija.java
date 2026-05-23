package com.eventsystem.event_management_system.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "lokacija")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Lokacija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lokacija_id")
    private Long lokacijaId;

    @Column(nullable = false, length = 200)
    private String naziv;

    @Column(nullable = false, length = 200)
    private String adresa;

    @Column(nullable = false, length = 100)
    private String grad;

    @Column(name = "ukupan_kapacitet", nullable = false)
    private Integer ukupanKapacitet;

    @Column(name = "bazna_cena_po_danu", nullable = false, precision = 12, scale = 2)
    private BigDecimal baznaCenaPoDanu;

    // V3: 1:N ka SALA — slab entitet, kaskada + orphanRemoval
    @OneToMany(mappedBy = "lokacija", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Sala> sale = new HashSet<>();

    // V4: 1:N ka DOGADJAJ — događaj je nezavisan, bez kaskade
    @OneToMany(mappedBy = "lokacija")
    private Set<Dogadjaj> dogadjaji = new HashSet<>();
}
