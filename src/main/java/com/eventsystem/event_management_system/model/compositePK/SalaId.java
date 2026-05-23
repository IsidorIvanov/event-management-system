package com.eventsystem.event_management_system.model.compositePK;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class SalaId {

    private Long lokacijaId;
    private String nazivSale;
}
