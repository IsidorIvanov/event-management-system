package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Ugovor;
import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UgovorRepository extends JpaRepository<Ugovor, Long> {

    List<Ugovor> findByDobavljacDobavljacId(Long dobavljacId);

    List<Ugovor> findByNabavkaNabavkaId(Long nabavkaId);

    Optional<Ugovor> findByBrojUgovora(String brojUgovora);

    boolean existsByBrojUgovora(String brojUgovora);

    @Query("""
            SELECT u
            FROM Ugovor u
            JOIN FETCH u.dobavljac d
            JOIN FETCH u.dogadjaj dg
            LEFT JOIN FETCH u.nabavka n
            WHERE u.ugovorId = :id
            """)
    Optional<Ugovor> findByIdWithDetalji(@Param("id") Long id);

    @Query("""
            SELECT u
            FROM Ugovor u
            JOIN FETCH u.dobavljac d
            JOIN FETCH u.dogadjaj dg
            LEFT JOIN FETCH u.nabavka n
            WHERE (:dobavljacId IS NULL OR d.dobavljacId = :dobavljacId)
              AND (:dogadjajId IS NULL OR dg.dogadjajId = :dogadjajId)
              AND (:status IS NULL OR u.status = :status)
            ORDER BY u.kreiranAt DESC
            """)
    List<Ugovor> findAllFiltered(
            @Param("dobavljacId") Long dobavljacId,
            @Param("dogadjajId") Long dogadjajId,
            @Param("status") StatusUgovora status
    );

    @Query("""
            SELECT u
            FROM Ugovor u
            JOIN FETCH u.dobavljac d
            JOIN FETCH u.dogadjaj dg
            LEFT JOIN FETCH u.nabavka n
            WHERE d.dobavljacId = :dobavljacId
              AND u.status = :status
              AND u.vaziDo >= :date
            ORDER BY u.vaziDo ASC
            """)
    List<Ugovor> findActiveByDobavljac(
            @Param("dobavljacId") Long dobavljacId,
            @Param("status") StatusUgovora status,
            @Param("date") LocalDate date
    );

    @Query("""
            SELECT u
            FROM Ugovor u
            WHERE u.status = com.eventsystem.event_management_system.utils.enums.StatusUgovora.AKTIVAN
              AND u.vaziDo < :danas
            """)
    List<Ugovor> findExpiredActive(@Param("danas") LocalDate danas);
}
