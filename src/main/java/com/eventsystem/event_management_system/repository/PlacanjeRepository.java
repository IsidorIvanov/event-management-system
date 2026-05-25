package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Placanje;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacanjeRepository extends JpaRepository<Placanje, Long> {
}
