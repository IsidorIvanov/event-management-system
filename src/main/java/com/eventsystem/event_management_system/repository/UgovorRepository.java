package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Ugovor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UgovorRepository extends JpaRepository<Ugovor, Long> {

    List<Ugovor> findByDobavljacDobavljacId(Long dobavljacId);

    List<Ugovor> findByNabavkaNabavkaId(Long nabavkaId);
}
