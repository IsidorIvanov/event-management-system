package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.FormatIzvestaja;
import com.eventsystem.event_management_system.utils.enums.IzvestajStatus;
import com.eventsystem.event_management_system.utils.enums.TipIzvestaja;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@Table(name = "finansijski_izvestaji")
@NoArgsConstructor
@AllArgsConstructor
public class FinansijskiIzvestaj {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "izvestaj_id")
    private Long izvestajId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipIzvestaja tip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FormatIzvestaja format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IzvestajStatus status;

    @Column(name = "dogadjaj_id")
    private Long dogadjajId;

    @Column(name = "klijent_id")
    private Long klijentId;

    @Column(name = "dobavljac_id")
    private Long dobavljacId;

    @Column(name = "period_od")
    private LocalDate periodOd;

    @Column(name = "period_do")
    private LocalDate periodDo;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "greska_poruka", columnDefinition = "TEXT")
    private String greskaPoruka;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kreirao_id", nullable = false)
    private Zaposleni kreirao;

    @Column(name = "kreiran_at", nullable = false)
    private LocalDateTime kreiranAt;

    @Column(name = "generisano_at")
    private LocalDateTime generisanoAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = IzvestajStatus.PENDING;
        }
        if (kreiranAt == null) {
            kreiranAt = LocalDateTime.now();
        }
    }
}
