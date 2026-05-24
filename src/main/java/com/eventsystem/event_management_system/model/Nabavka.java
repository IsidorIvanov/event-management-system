package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.KriterijumSelekcijeDobavljaca;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Getter
@Setter
@Table(name = "nabavka")
@NoArgsConstructor
@AllArgsConstructor
public class Nabavka {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nabavka_id")
    private Long nabavkaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private Dogadjaj dogadjaj;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kreirao_id", nullable = false)
    private Zaposleni kreirao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dobavljac_id")
    private Dobavljac dobavljac;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusNabavke status;

    @Column(name = "ukupna_cena", precision = 14, scale = 2)
    private BigDecimal ukupnaCena;

    @Column(name = "kreiran_at", nullable = false)
    private LocalDateTime kreiranAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "selekcija_kriterijum", length = 30)
    private KriterijumSelekcijeDobavljaca selekcijaKriterijum;

    @OneToMany(mappedBy = "nabavka", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StavkaNabavke> stavke = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = StatusNabavke.NACRT;
        }
        if (kreiranAt == null) {
            kreiranAt = LocalDateTime.now();
        }
        if (ukupnaCena == null) {
            ukupnaCena = BigDecimal.ZERO;
        }
    }
}
