package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.model.compositePK.TipKarteId;
import com.eventsystem.event_management_system.utils.VrstaKarte;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Builder
@Table(name = "tip_karte")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class TipKarte {

    @EmbeddedId
    private TipKarteId id;

    @MapsId("dogadjajId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id")
    private Dogadjaj dogadjaj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VrstaKarte vrsta;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cena;

    @Column(nullable = false)
    private Integer kvota;

    @Column(length = 500)
    private String opis;
}
