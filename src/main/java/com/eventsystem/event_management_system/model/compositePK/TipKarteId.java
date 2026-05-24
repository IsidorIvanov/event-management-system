package com.eventsystem.event_management_system.model.compositePK;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class TipKarteId implements Serializable {

    @Column(name = "dogadjaj_id")
    private Long dogadjajId;

    @Column(name = "naziv_tipa", length = 100)
    private String nazivTipa;
}
