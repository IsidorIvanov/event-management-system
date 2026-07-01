package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.utils.enums.StatusDobavljaca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DobavljacRepository extends JpaRepository<Dobavljac, Long> {

    List<Dobavljac> findByStatus(StatusDobavljaca status);

    boolean existsByPib(String pib);

    boolean existsByPibAndDobavljacIdNot(String pib, Long dobavljacId);
}
