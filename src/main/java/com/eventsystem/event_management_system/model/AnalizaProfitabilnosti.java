package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.AnalizaStatus;
import com.eventsystem.event_management_system.utils.enums.RezultatOcene;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@Table(name = "analize_profitabilnosti")
@Check(constraints = "ukupan_prihod >= 0 and ukupan_trosak >= 0")
@NoArgsConstructor
@AllArgsConstructor
public class AnalizaProfitabilnosti {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analiza_id")
    private Long analizaId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private Dogadjaj dogadjaj;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kreirao_id", nullable = false)
    private Zaposleni kreirao;

    @Column(name = "ukupan_prihod", nullable = false, precision = 15, scale = 2)
    private BigDecimal ukupanPrihod;

    @Column(name = "ukupan_trosak", nullable = false, precision = 15, scale = 2)
    private BigDecimal ukupanTrosak;

    @Column(name = "datum_analize", nullable = false)
    private LocalDate datumAnalize;

    @Column(name = "finalizovano_at")
    private LocalDateTime finalizovanoAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "rezultat_ocene", length = 20)
    private RezultatOcene rezultatOcene;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalizaStatus status;

    @Column(columnDefinition = "TEXT")
    private String napomene;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = AnalizaStatus.DRAFT;
        }
        if (datumAnalize == null) {
            datumAnalize = LocalDate.now();
        }
        if (ukupanPrihod == null) {
            ukupanPrihod = BigDecimal.ZERO;
        }
        if (ukupanTrosak == null) {
            ukupanTrosak = BigDecimal.ZERO;
        }
    }
}
