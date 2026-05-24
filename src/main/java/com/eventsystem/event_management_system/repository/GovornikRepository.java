package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Govornik;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GovornikRepository extends JpaRepository<Govornik, Long> {

    boolean existsByEmail(String email);
}
