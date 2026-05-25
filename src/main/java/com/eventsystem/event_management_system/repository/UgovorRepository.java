package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Ugovor;
import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface UgovorRepository extends JpaRepository<Ugovor, Long> {

    List<Ugovor> findByDobavljacDobavljacId(Long dobavljacId);

    List<Ugovor> findByNabavkaNabavkaId(Long nabavkaId);

    @Query("""
            SELECT COUNT(u) > 0
            FROM Ugovor u
            WHERE u.dobavljac.dobavljacId = :dobavljacId
              AND u.status = :status
              AND (u.vaziDo IS NULL OR u.vaziDo >= :date)
            """)
    boolean existsActiveForDobavljac(
            @Param("dobavljacId") Long dobavljacId,
            @Param("status") StatusUgovora status,
            @Param("date") LocalDate date
    );
}
