package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Dogadjaj;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DogadjajRepository extends JpaRepository<Dogadjaj, Long> {

    @Query("SELECT d FROM Dogadjaj d JOIN FETCH d.lokacija")
    List<Dogadjaj> findAllWithLokacija();
}