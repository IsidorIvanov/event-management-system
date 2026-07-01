package com.eventsystem.event_management_system.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "ucesnik_sesija",
    uniqueConstraints = @UniqueConstraint(columnNames = {"korisnik_id", "sesija_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UcesnikSesija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "korisnik_id", nullable = false)
    private Ucesnik ucesnik;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesija_id", nullable = false)
    private Sesija sesija;

    @Column(name = "datum_dodavanja")
    private LocalDateTime datumDodavanja;

    @PrePersist
    protected void onCreate() {
        if (datumDodavanja == null) datumDodavanja = LocalDateTime.now();
    }
}

