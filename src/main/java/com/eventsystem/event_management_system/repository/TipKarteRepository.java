package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.TipKarte;
import com.eventsystem.event_management_system.model.compositePK.TipKarteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipKarteRepository extends JpaRepository<TipKarte, TipKarteId> {

    List<TipKarte> findAllByDogadjaj_DogadjajId(Long dogadjajId);

    boolean existsByDogadjaj_DogadjajIdAndId_NazivTipa(Long dogadjajId, String nazivTipa);
}
