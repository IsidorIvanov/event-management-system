package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.model.compositePK.SalaId;
import com.eventsystem.event_management_system.utils.TipSale;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@Table(name = "sala")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Sala {

    @EmbeddedId
    private SalaId id;

    @MapsId("lokacijaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lokacija_id")
    private Lokacija lokacija;

    @Enumerated(EnumType.STRING)
    @Column(name = "tip_sale", nullable = false, length = 20)
    private TipSale tipSale;

    @Column(nullable = false)
    private Integer kapacitet;

    @Column(name = "bazna_cena_po_danu", nullable = false, precision = 12, scale = 2)
    private BigDecimal baznaCenaPoDanu;

    // V22: inverzna strana ka SESIJA
    @OneToMany(mappedBy = "sala")
    private Set<Sesija> sesije = new HashSet<>();
}
