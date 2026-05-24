package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Sesija;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SesijaRepository extends JpaRepository<Sesija, Long> {

    List<Sesija> findAllByDogadjaj_DogadjajId(Long dogadjajId);
}
