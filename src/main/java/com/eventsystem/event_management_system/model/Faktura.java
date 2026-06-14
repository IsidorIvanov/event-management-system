package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Getter
@Setter
@Table(name = "faktura")
@NoArgsConstructor
@AllArgsConstructor
public class Faktura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faktura_id")
    private Long fakturaId;

    @Column(name = "broj_fakture", nullable = false, unique = true, length = 60)
    private String brojFakture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipFakture tip;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FakturaStatus status;

    @Column(name = "ukupna_iznos", precision = 14, scale = 2)
    private BigDecimal ukupnaIznos;

    @Column(name = "placeni_iznos", precision = 14, scale = 2)
    private BigDecimal placeniIznos;

    @Column(name = "datum_izdavanja")
    private LocalDate datumIzdavanja;

    @Column(name = "dogadjaj_id")
    private Long dogadjajId;

    @Column(name = "klijent_id")
    private Long klijentId;

    @Column(name = "dobavljac_id")
    private Long dobavljacId;

    @Column(name = "ugovor_id")
    private Long ugovorId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ugovor_id", insertable = false, updatable = false)
    private Ugovor ugovor;

    @Column(name = "kreirao_id")
    private Long kreiraoId;

    @Column(name = "rok_placanja")
    private LocalDate rokPlacanja;

    @Column(columnDefinition = "TEXT")
    private String napomena;

    @OneToMany(mappedBy = "faktura", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StavkaFakture> stavke = new ArrayList<>();

    @OneToMany(mappedBy = "faktura", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Placanje> placanja = new ArrayList<>();

    @Column(name = "kreiran_at", nullable = false)
    private LocalDateTime kreiranAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = FakturaStatus.DRAFT;
        if (placeniIznos == null) placeniIznos = BigDecimal.ZERO;
        if (kreiranAt == null) kreiranAt = LocalDateTime.now();
    }
}
