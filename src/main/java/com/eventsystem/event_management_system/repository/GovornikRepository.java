package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Govornik;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface GovornikRepository extends JpaRepository<Govornik, Long> {

    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT g FROM Govornik g LEFT JOIN FETCH g.sesije")
    List<Govornik> findAllWithSesije();

    @Query("SELECT DISTINCT g FROM Govornik g LEFT JOIN FETCH g.sesije s WHERE s.dogadjaj.dogadjajId = :dogadjajId")
    List<Govornik> findDistinctBySesijeDogadjajDogadjajId(@Param("dogadjajId") Long dogadjajId);

    @Query("""
            SELECT COALESCE(SUM(g.honorar), 0)
            FROM Govornik g
            WHERE EXISTS (
                SELECT 1
                FROM g.sesije s
                WHERE s.dogadjaj.dogadjajId = :dogadjajId
            )
            """)
    BigDecimal sumHonorarByDogadjaj(@Param("dogadjajId") Long dogadjajId);
}


