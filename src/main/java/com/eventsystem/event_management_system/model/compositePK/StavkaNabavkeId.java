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
public class StavkaNabavkeId {

    @Column(name = "nabavka_id")
    private Long nabavkaId;

    @Column(name = "redni_broj")
    private Integer redniBroj;
}
