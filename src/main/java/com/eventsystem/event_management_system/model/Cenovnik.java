package com.eventsystem.event_management_system.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Builder
@Getter
@Setter
@Table(name = "cenovnik")
@NoArgsConstructor
@AllArgsConstructor
public class Cenovnik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cenovnik_id")
    private Long cenovnikId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dobavljac_id", nullable = false)
    private Dobavljac dobavljac;

    @Column(name = "naziv_resursa", nullable = false, length = 200)
    private String nazivResursa;

    @Column(columnDefinition = "TEXT")
    private String opis;

    @Column(name = "jedinica_mere", nullable = false, length = 30)
    private String jedinicaMere;

    @Column(name = "cena_jedinicna", nullable = false, precision = 12, scale = 2)
    private BigDecimal cenaJedinicna;

    @Column(nullable = false)
    private Boolean dostupnost;

    @PrePersist
    protected void onCreate() {
        if (dostupnost == null) {
            dostupnost = true;
        }
    }
}
