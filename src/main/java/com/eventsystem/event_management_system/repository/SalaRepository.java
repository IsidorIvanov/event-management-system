package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Sala;
import com.eventsystem.event_management_system.model.compositePK.SalaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, SalaId> {

    Optional<Sala> findByLokacijaLokacijaIdAndIdNazivSale(Long lokacijaId, String nazivSale);

    List<Sala> findByLokacija_LokacijaId(Long lokacijaId);
}
