package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.model.compositePK.StavkaNabavkeId;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Builder
@Getter
@Setter
@Table(name = "stavka_nabavke")
@NoArgsConstructor
@AllArgsConstructor
public class StavkaNabavke {

    @EmbeddedId
    private StavkaNabavkeId id;

    @MapsId("nabavkaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nabavka_id")
    private Nabavka nabavka;

    @Column(name = "naziv_resursa", nullable = false, length = 200)
    private String nazivResursa;

    @Column(nullable = false)
    private Integer kolicina;

    @Column(name = "jedinicna_cena", nullable = false, precision = 12, scale = 2)
    private BigDecimal jedinicnaCena;

    @Column(name = "ukupna_cena", nullable = false, precision = 14, scale = 2)
    private BigDecimal ukupnaCena;

    @Column(columnDefinition = "TEXT")
    private String opis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cenovnik_id")
    private Cenovnik cenovnik;

    @PrePersist
    @PreUpdate
    private void izracunajUkupnu() {
        if (kolicina != null && jedinicnaCena != null) {
            ukupnaCena = jedinicnaCena.multiply(BigDecimal.valueOf(kolicina));
        }
    }

    public void osveziUkupnuCenu() {
        izracunajUkupnu();
    }
}
