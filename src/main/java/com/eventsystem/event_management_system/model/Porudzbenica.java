package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.StatusPorudzbenice;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Getter
@Setter
@Table(name = "porudzbenica")
@NoArgsConstructor
@AllArgsConstructor
public class Porudzbenica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "porudzbenica_id")
    private Long porudzbenicaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nabavka_id", nullable = false)
    private Nabavka nabavka;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dobavljac_id", nullable = false)
    private Dobavljac dobavljac;

    @Column(name = "broj_porudzbenice", nullable = false, unique = true, length = 30)
    private String brojPorudzbenice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPorudzbenice status;

    @Column(name = "ukupna_cena", precision = 14, scale = 2)
    private BigDecimal ukupnaCena;

    @Column(name = "kreiran_at", nullable = false)
    private LocalDateTime kreiranAt;

    @Column(name = "poslata_at")
    private LocalDateTime poslataAt;

    @Column(name = "potvrdjena_at")
    private LocalDateTime potvrdjenaAt;

    @Column(name = "rok_isporuke")
    private LocalDate rokIsporuke;

    @Column(name = "isporucena_at")
    private LocalDateTime isporucenaAt;

    @Column(columnDefinition = "TEXT")
    private String napomena;

    @OneToMany(mappedBy = "porudzbenica", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StavkaPorudzbenice> stavke = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = StatusPorudzbenice.KREIRANA;
        }
        if (kreiranAt == null) {
            kreiranAt = LocalDateTime.now();
        }
    }
}
