package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.TipTroska;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@Table(name = "trosak")
@Check(constraints = "iznos >= 0 and refundirani_iznos >= 0 and refundirani_iznos <= iznos")
@NoArgsConstructor
@AllArgsConstructor
public class Trosak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trosak_id")
    private Long trosakId;

    private String opis;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal iznos;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TipTroska tip;


    @Column(name = "faktura_id")
    private Long fakturaId;

    @Column(name = "stavka_fakture_id")
    private Long stavkaFaktureId;

    @Column(name = "budzet_id", nullable = false)
    private Long budzetId;

    @Column(name = "kategorija_id", nullable = false)
    private Long kategorijaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(
                    name = "budzet_id",
                    referencedColumnName = "budzet_id",
                    insertable = false,
                    updatable = false,
                    nullable = false
            ),
            @JoinColumn(
                    name = "kategorija_id",
                    referencedColumnName = "kategorija_id",
                    insertable = false,
                    updatable = false,
                    nullable = false
            )
    })
    private StavkaBudzeta stavkaBudzeta;

    @Column(name = "dogadjaj_id", nullable = false)
    private Long dogadjajId;

    @Column(name = "dobavljac_id")
    private Long dobavljacId;

    @Column(name = "evidentirao_id")
    private Long evidentiraoId;

    @Column(name = "datum_troska", nullable = false)
    private java.time.LocalDate datumTroska;

    @Column(name = "refundirani_iznos", nullable = false, precision = 14, scale = 2)
    private BigDecimal refundiraniIznos;

    @Column(name = "kreiran_at", nullable = false)
    private LocalDateTime kreiranAt;

    @PrePersist
    protected void onCreate() {
        if (refundiraniIznos == null) refundiraniIznos = BigDecimal.ZERO;
        if (kreiranAt == null) kreiranAt = LocalDateTime.now();
    }
}
