package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.TipScenarija;
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
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@Table(name = "scenariji_prognoze")
@Check(constraints = "projektovani_prihod >= 0 and projektovani_trosak >= 0")
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioPrognoze {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_id")
    private Long scenarioId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private Dogadjaj dogadjaj;

    @Column(name = "naziv_scenarija", nullable = false, length = 200)
    private String nazivScenarija;

    @Enumerated(EnumType.STRING)
    @Column(name = "tip_scenarija", nullable = false, length = 20)
    private TipScenarija tipScenarija;

    @Column(name = "projektovani_prihod", nullable = false, precision = 15, scale = 2)
    private BigDecimal projektovaniPrihod;

    @Column(name = "projektovani_trosak", nullable = false, precision = 15, scale = 2)
    private BigDecimal projektovaniTrosak;

    @Column(columnDefinition = "TEXT")
    private String pretpostavke;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
