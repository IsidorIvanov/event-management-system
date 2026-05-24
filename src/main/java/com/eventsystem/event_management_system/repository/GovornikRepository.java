package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Govornik;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GovornikRepository extends JpaRepository<Govornik, Long> {

    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT g FROM Govornik g LEFT JOIN FETCH g.sesije")
    List<Govornik> findAllWithSesije();

    @Query("SELECT DISTINCT g FROM Govornik g LEFT JOIN FETCH g.sesije s WHERE s.dogadjaj.dogadjajId = :dogadjajId")
    List<Govornik> findDistinctBySesijeDogadjajDogadjajId(@Param("dogadjajId") Long dogadjajId);
}


