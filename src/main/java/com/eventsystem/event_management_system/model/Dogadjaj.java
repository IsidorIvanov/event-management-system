package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@Getter @Setter
@Table(name = "dogadjaj")
@NoArgsConstructor @AllArgsConstructor
public class Dogadjaj {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dogadjaj_id")
    private Long dogadjajId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lokacija_id", nullable = false)
    private Lokacija lokacija;

    @Column(nullable = false, length = 200)
    private String naziv;

    @Column(name = "datum_pocetka", nullable = false)
    private LocalDate datumPocetka;

    @Column(name = "datum_zavrsetka", nullable = false)
    private LocalDate datumZavrsetka;

    @Column(name = "maks_kapacitet", nullable = false)
    private Integer maksKapacitet;

    @Column(columnDefinition = "TEXT")
    private String opis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDogadjaja status;

    /** Da li je već poslat podsetnik „događaj uskoro" (P1) — sprečava ponovno slanje. */
    @Column(name = "podsetnik_poslat", columnDefinition = "boolean default false")
    private boolean podsetnikPoslat;

    /**
     * Tagovi/teme događaja (npr. "Tehnologija", "Biznis") koji opisuju o čemu se
     * radi na događaju. Koriste se za sistem preporuka — porede se sa interesima
     * učesnika. Čuvaju se kao slobodne oznake u zasebnoj tabeli.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "dogadjaj_tag",
            joinColumns = @JoinColumn(name = "dogadjaj_id")
    )
    @Column(name = "tag", length = 100, nullable = false)
    @Builder.Default
    private Set<String> tagovi = new HashSet<>();

    @OneToMany(mappedBy = "dogadjaj", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TipKarte> tipoviKarata = new HashSet<>();

    @OneToMany(mappedBy = "dogadjaj", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Sesija> sesije = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = StatusDogadjaja.DRAFT;
        }
    }
}
