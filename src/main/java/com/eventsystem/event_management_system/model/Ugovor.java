package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@Table(name = "ugovor")
@NoArgsConstructor
@AllArgsConstructor
public class Ugovor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ugovor_id")
    private Long ugovorId;

    @Column(name = "broj_ugovora", nullable = false, unique = true, length = 64)
    private String brojUgovora;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dobavljac_id", nullable = false)
    private Dobavljac dobavljac;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private Dogadjaj dogadjaj;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nabavka_id")
    private Nabavka nabavka;

    @Column(name = "datum_potpisivanja", nullable = false)
    private LocalDate datumPotpisivanja;

    @Column(name = "vrednost", nullable = false, precision = 15, scale = 2)
    private BigDecimal vrednost;

    @Column(nullable = false, length = 500)
    private String predmet;

    @Column(name = "uslovi_placanja", columnDefinition = "TEXT")
    private String usloviPlacanja;

    @Column(name = "dokument_url", length = 500)
    private String dokumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusUgovora status;

    @Column(name = "kreiran_at", nullable = false)
    private LocalDateTime kreiranAt;

    @Column(name = "vazi_do", nullable = false)
    private LocalDate vaziDo;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = StatusUgovora.NACRT;
        }
        if (kreiranAt == null) {
            kreiranAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
