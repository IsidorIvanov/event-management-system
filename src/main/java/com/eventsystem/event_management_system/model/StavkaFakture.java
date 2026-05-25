package com.eventsystem.event_management_system.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Builder
@Getter
@Setter
@Table(name = "stavka_fakture")
@NoArgsConstructor
@AllArgsConstructor
public class StavkaFakture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stavka_fakture_id")
    private Long stavkaFaktureId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faktura_id", nullable = false)
    private Faktura faktura;

    private Integer redniBroj;

    private String naziv;

    @Column(precision = 10, scale = 3)
    private BigDecimal kolicina;

    @Column(precision = 14, scale = 2)
    private BigDecimal jedinicnaCena;

    @Column(precision = 14, scale = 2)
    private BigDecimal ukupnaCena;

    @Column(columnDefinition = "TEXT")
    private String napomena;
    
    @Column(name = "budzet_id")
    private Long budzetId;

    @Column(name = "kategorija_id")
    private Long kategorijaId;

    @Column(name = "trosak_id")
    private Long trosakId;
}
