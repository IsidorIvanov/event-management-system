package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Dogadjaj;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DogadjajRepository extends JpaRepository<Dogadjaj, Long> {

    @Query("SELECT d FROM Dogadjaj d JOIN FETCH d.lokacija")
    List<Dogadjaj> findAllWithLokacija();

    @Query("SELECT d FROM Dogadjaj d JOIN FETCH d.lokacija WHERE d.dogadjajId = :id")
    Optional<Dogadjaj> findByIdWithLokacija(@Param("id") Long id);
}