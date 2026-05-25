package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Klijent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KlijentRepository extends JpaRepository<Klijent, Long> {
}
