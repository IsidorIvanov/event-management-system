package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Lokacija;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LokacijaRepository extends JpaRepository<Lokacija, Long> {
}