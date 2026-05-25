package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.TipTroska;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@Table(name = "trosak")
@NoArgsConstructor
@AllArgsConstructor
public class Trosak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trosak_id")
    private Long trosakId;

    private String opis;

    @Column(precision = 14, scale = 2)
    private BigDecimal iznos;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TipTroska tip;


    @Column(name = "faktura_id")
    private Long fakturaId;

    @Column(name = "stavka_fakture_id")
    private Long stavkaFaktureId;

    @Column(name = "budzet_id")
    private Long budzetId;

    @Column(name = "kategorija_id")
    private Long kategorijaId;

    @Column(name = "dogadjaj_id")
    private Long dogadjajId;

    @Column(name = "dobavljac_id")
    private Long dobavljacId;

    @Column(name = "evidentirao_id")
    private Long evidentiraoId;

    @Column(name = "datum_troska")
    private java.time.LocalDate datumTroska;

    @Column(name = "refundirani_iznos", precision = 14, scale = 2)
    private BigDecimal refundiraniIznos;

    @Column(name = "kreiran_at", nullable = false)
    private LocalDateTime kreiranAt;

    @PrePersist
    protected void onCreate() {
        if (refundiraniIznos == null) refundiraniIznos = BigDecimal.ZERO;
        if (kreiranAt == null) kreiranAt = LocalDateTime.now();
    }
}
