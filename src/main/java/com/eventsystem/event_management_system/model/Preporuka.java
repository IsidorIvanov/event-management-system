package com.eventsystem.event_management_system.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Personalizovana preporuka generisana za učesnika. U ovom sistemu preporučuju
 * se isključivo događaji (nema tipa preporuke ni sesija), pa entitet ne sadrži
 * {@code sesija_id} ni {@code tip} enumeraciju.
 *
 * <p>Skor poklapanja i obrazloženje se računaju upoređivanjem interesovanja
 * učesnika ({@link Ucesnik#getInteresi()}) sa tagovima događaja
 * ({@link Dogadjaj#getTagovi()}).</p>
 */
@Entity
@Builder
@Getter @Setter
@Table(name = "preporuka")
@NoArgsConstructor @AllArgsConstructor
public class Preporuka {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preporuka_id")
    private Long preporukaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "korisnik_id", nullable = false)
    private Ucesnik ucesnik;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private Dogadjaj dogadjaj;

    /** Procenat poklapanja sa profilom učesnika (0–100). */
    @Column(name = "skor_poklapanja", nullable = false, precision = 5, scale = 2)
    private BigDecimal skorPoklapanja;

    /** Tekstualno obrazloženje zašto je događaj preporučen. */
    @Column(columnDefinition = "TEXT")
    private String razlog;

    @Column(name = "datum_generisanja", nullable = false)
    private LocalDate datumGenerisanja;

    @PrePersist
    protected void onCreate() {
        if (datumGenerisanja == null) {
            datumGenerisanja = LocalDate.now();
        }
    }
}
