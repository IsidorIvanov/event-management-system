package com.eventsystem.event_management_system.model.compositePK;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StavkaBudzetaId {

    @Column(name = "budzet_id")
    private Long budzetId;

    @Column(name = "kategorija_id")
    private Long kategorijaId;
}
