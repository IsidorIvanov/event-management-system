package com.eventsystem.event_management_system.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@Table(name = "govornik")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Govornik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "govornik_id")
    private Long govornikId;

    @Column(nullable = false, length = 100)
    private String ime;

    @Column(nullable = false, length = 100)
    private String prezime;

    @Column(length = 200)
    private String kompanija;

    @Column(length = 100)
    private String pozicija;

    @Column(columnDefinition = "TEXT")
    private String biografija;

    @Column(length = 150)
    private String email;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal honorar;

    // V23: M:N sa SESIJA — inverzna strana, vlasnik je Sesija
    @ManyToMany(mappedBy = "govornici")
    private Set<Sesija> sesije = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (honorar == null) {
            honorar = BigDecimal.ZERO;
        }
    }
}
