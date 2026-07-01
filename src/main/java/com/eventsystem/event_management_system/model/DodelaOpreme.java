package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.StatusDodeleOpreme;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@Table(name = "dodela_opreme")
@NoArgsConstructor
@AllArgsConstructor
public class DodelaOpreme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dodela_id")
    private Long dodelaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oprema_id", nullable = false)
    private Oprema oprema;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private Dogadjaj dogadjaj;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesija_id")
    private Sesija sesija;

    @Column(nullable = false)
    private Integer kolicina;

    @Column(name = "datum_od", nullable = false)
    private LocalDate datumOd;

    @Column(name = "datum_do", nullable = false)
    private LocalDate datumDo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDodeleOpreme status;

    @Column(columnDefinition = "TEXT")
    private String napomena;

    @Column(name = "kreiran_at", nullable = false)
    private LocalDateTime kreiranAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = StatusDodeleOpreme.REZERVISANO;
        }
        if (kreiranAt == null) {
            kreiranAt = LocalDateTime.now();
        }
    }
}
