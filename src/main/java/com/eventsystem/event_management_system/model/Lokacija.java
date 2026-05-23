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

    @Column(nullable = false)
    private String drzava;

    @OneToMany(mappedBy = "lokacija", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Sala> sale = new HashSet<>();

    @OneToMany(mappedBy = "lokacija")
    private Set<Dogadjaj> dogadjaji = new HashSet<>();
}
