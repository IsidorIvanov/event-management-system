package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
import com.eventsystem.event_management_system.utils.enums.StatusNotifikacije;
import com.eventsystem.event_management_system.utils.enums.TipNotifikacije;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Pojedinačno obaveštenje poslato učesniku — automatska komunikacija o
 * sesijama, događaju ili podsetnicima.
 *
 * <p>Tip {@code PREPORUKA} i kanal {@code SMS} se namerno ne implementiraju
 * (vidi {@link TipNotifikacije} i {@link KanalNotifikacije}).</p>
 */
@Entity
@Builder
@Getter @Setter
@Table(name = "notifikacija")
@NoArgsConstructor @AllArgsConstructor
public class Notifikacija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notifikacija_id")
    private Long notifikacijaId;

    /** Učesnik koji prima obaveštenje. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "korisnik_id", nullable = false)
    private Ucesnik korisnik;

    /** Događaj na koji se obaveštenje odnosi (opciono). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dogadjaj_id")
    private Dogadjaj dogadjaj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipNotifikacije tip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KanalNotifikacije kanal;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sadrzaj;

    @Column(name = "vreme_slanja", nullable = false)
    private LocalDateTime vremeSlanja;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusNotifikacije status;

    @PrePersist
    protected void onCreate() {
        if (vremeSlanja == null) {
            vremeSlanja = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusNotifikacije.NEPROCITANO;
        }
    }
}
