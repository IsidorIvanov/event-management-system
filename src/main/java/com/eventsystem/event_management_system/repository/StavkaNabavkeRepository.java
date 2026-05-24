package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.StavkaNabavke;
import com.eventsystem.event_management_system.model.compositePK.StavkaNabavkeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StavkaNabavkeRepository extends JpaRepository<StavkaNabavke, StavkaNabavkeId> {

    List<StavkaNabavke> findByNabavkaNabavkaIdOrderByIdRedniBrojAsc(Long nabavkaId);
}
