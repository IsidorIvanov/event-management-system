package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.ScenarioPrognoze;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScenarioPrognozeRepository extends JpaRepository<ScenarioPrognoze, Long> {

    @Query("""
            SELECT s
            FROM ScenarioPrognoze s
            JOIN FETCH s.dogadjaj d
            WHERE d.dogadjajId = :dogadjajId
            ORDER BY s.createdAt DESC
            """)
    List<ScenarioPrognoze> findByDogadjajDogadjajId(@Param("dogadjajId") Long dogadjajId);

    @Query("""
            SELECT s
            FROM ScenarioPrognoze s
            JOIN FETCH s.dogadjaj d
            WHERE s.scenarioId = :scenarioId
            """)
    Optional<ScenarioPrognoze> findByIdWithDogadjaj(@Param("scenarioId") Long scenarioId);
}
