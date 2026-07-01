package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.StatusOpreme;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@Table(name = "oprema")
@NoArgsConstructor
@AllArgsConstructor
public class Oprema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oprema_id")
    private Long opremaId;

    @Column(nullable = false, length = 200)
    private String naziv;

    @Column(nullable = false, length = 80)
    private String kategorija;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cenovnik_id")
    private Cenovnik cenovnik;

    @Column(name = "kolicina_ukupno", nullable = false)
    private Integer kolicinaUkupno;

    @Column(name = "kolicina_dostupno", nullable = false)
    private Integer kolicinaDostupno;

    @Column(name = "kolicina_rezervisano", nullable = false)
    private Integer kolicinaRezervisano;

    @Column(name = "kolicina_na_dogadjaju", nullable = false)
    private Integer kolicinaNaDogadjaju;

    @Column(name = "kolicina_u_servisu", nullable = false)
    private Integer kolicinaUServisu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOpreme status;

    @Column(length = 100)
    private String lokacijaSkladista;

    @Column(name = "serijski_broj", length = 80)
    private String serijskiBroj;

    @Column(columnDefinition = "TEXT")
    private String napomena;

    @PrePersist
    protected void onCreate() {
        if (kolicinaUkupno == null) kolicinaUkupno = 0;
        if (kolicinaDostupno == null) kolicinaDostupno = 0;
        if (kolicinaRezervisano == null) kolicinaRezervisano = 0;
        if (kolicinaNaDogadjaju == null) kolicinaNaDogadjaju = 0;
        if (kolicinaUServisu == null) kolicinaUServisu = 0;
        if (status == null) status = StatusOpreme.DOSTUPNO;
    }
}
