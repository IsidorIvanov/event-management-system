package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.model.compositePK.StavkaBudzetaId;
import com.eventsystem.event_management_system.utils.enums.StatusKontrole;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Builder
@Getter
@Setter
@Table(name = "stavka_budzeta")
@Check(constraints = "planirani_iznos >= 0 and stvarni_iznos >= 0 and prag_kriticnog > prag_upozorenja")
@NoArgsConstructor
@AllArgsConstructor
public class StavkaBudzeta {

    @EmbeddedId
    private StavkaBudzetaId id;

    @MapsId("budzetId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budzet_id")
    private Budzet budzet;

    @MapsId("kategorijaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kategorija_id")
    private BudzetKategorija kategorija;

    @Column(name = "planirani_iznos", nullable = false, precision = 15, scale = 2)
    private BigDecimal planiraniIznos;

    @Column(name = "stvarni_iznos", nullable = false, precision = 15, scale = 2)
    private BigDecimal stvarniIznos;

    @Column(name = "prag_upozorenja", nullable = false, precision = 5, scale = 4)
    private BigDecimal pragUpozorenja;

    @Column(name = "prag_kriticnog", nullable = false, precision = 5, scale = 4)
    private BigDecimal pragKriticnog;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_kontrole", nullable = false, length = 16)
    private StatusKontrole statusKontrole;

    @Column(length = 500)
    private String komentar;

    @PrePersist
    @PreUpdate
    protected void applyDefaultsAndStatus() {
        if (stvarniIznos == null) {
            stvarniIznos = BigDecimal.ZERO;
        }
        if (pragUpozorenja == null) {
            pragUpozorenja = new BigDecimal("0.8000");
        }
        if (pragKriticnog == null) {
            pragKriticnog = new BigDecimal("0.9500");
        }
        if (statusKontrole == null) {
            statusKontrole = StatusKontrole.NA_PLANU;
        }
    }
}
