package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.model.compositePK.StavkaPorudzbeniceId;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Builder
@Getter
@Setter
@Table(name = "stavka_porudzbenice")
@NoArgsConstructor
@AllArgsConstructor
public class StavkaPorudzbenice {

    @EmbeddedId
    private StavkaPorudzbeniceId id;

    @MapsId("porudzbenicaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "porudzbenica_id")
    private Porudzbenica porudzbenica;

    @Column(name = "naziv_resursa", nullable = false, length = 200)
    private String nazivResursa;

    @Column(nullable = false)
    private Integer kolicina;

    @Column(name = "jedinicna_cena", nullable = false, precision = 12, scale = 2)
    private BigDecimal jedinicnaCena;

    @Column(name = "ukupna_cena", nullable = false, precision = 14, scale = 2)
    private BigDecimal ukupnaCena;

    @Column(name = "razlog_potrebe", length = 300)
    private String razlogPotrebe;

    @PrePersist
    @PreUpdate
    private void izracunajUkupnu() {
        if (kolicina != null && jedinicnaCena != null) {
            ukupnaCena = jedinicnaCena.multiply(BigDecimal.valueOf(kolicina));
        }
    }
}
