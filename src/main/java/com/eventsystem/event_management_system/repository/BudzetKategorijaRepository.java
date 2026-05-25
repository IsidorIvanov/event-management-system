package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.BudzetKategorija;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BudzetKategorijaRepository extends JpaRepository<BudzetKategorija, Long> {

    boolean existsByNazivIgnoreCase(String naziv);

    Optional<BudzetKategorija> findByNazivIgnoreCase(String naziv);
}
