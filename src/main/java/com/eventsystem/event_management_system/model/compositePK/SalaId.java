package com.eventsystem.event_management_system.model.compositePK;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class SalaId {

    @Column(name = "lokacija_id")
    private Long lokacijaId;

    @Column(name = "naziv_sale", length = 100)
    private String nazivSale;
}
