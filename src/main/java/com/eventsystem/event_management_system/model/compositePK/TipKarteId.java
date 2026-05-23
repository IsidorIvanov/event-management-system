package com.eventsystem.event_management_system.model.compositePK;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class TipKarteId implements Serializable {

    private Long dogadjajId;
    private String nazivTipa;
}
