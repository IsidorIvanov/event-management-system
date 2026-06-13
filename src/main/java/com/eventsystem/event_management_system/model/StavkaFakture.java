package com.eventsystem.event_management_system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Builder
@Getter
@Setter
@Table(name = "stavka_fakture")
@Check(constraints = "kolicina > 0 and jedinicna_cena >= 0 and ukupna_cena >= 0")
@NoArgsConstructor
@AllArgsConstructor
public class StavkaFakture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stavka_fakture_id")
    private Long stavkaFaktureId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faktura_id", nullable = false)
    private Faktura faktura;

    private Integer redniBroj;

    private String naziv;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal kolicina;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal jedinicnaCena;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ukupnaCena;

    @Column(columnDefinition = "TEXT")
    private String napomena;
    
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

    @Column(name = "trosak_id")
    private Long trosakId;
}
