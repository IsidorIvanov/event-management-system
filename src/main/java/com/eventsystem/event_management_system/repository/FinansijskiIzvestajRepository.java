package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.FinansijskiIzvestaj;
import com.eventsystem.event_management_system.model.Zaposleni;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FinansijskiIzvestajRepository extends JpaRepository<FinansijskiIzvestaj, Long> {

    @Query("""
            SELECT i FROM FinansijskiIzvestaj i
            JOIN FETCH i.kreirao
            WHERE i.kreirao = :kreirao
            ORDER BY i.kreiranAt DESC
            """)
    List<FinansijskiIzvestaj> findByKreiraoOrderByKreiranAtDesc(@Param("kreirao") Zaposleni kreirao);

    @Query("""
            SELECT i FROM FinansijskiIzvestaj i
            JOIN FETCH i.kreirao
            WHERE i.izvestajId = :id
            """)
    Optional<FinansijskiIzvestaj> findByIdWithKreirao(@Param("id") Long id);
}
