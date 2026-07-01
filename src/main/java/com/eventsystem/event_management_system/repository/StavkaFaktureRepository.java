package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.StavkaFakture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface StavkaFaktureRepository extends JpaRepository<StavkaFakture, Long> {

    List<StavkaFakture> findByFakturaFakturaId(Long fakturaId);

    @Query("""
            SELECT COALESCE(SUM(s.ukupnaCena), 0)
            FROM StavkaFakture s
            WHERE s.faktura.fakturaId = :fakturaId
            """)
    BigDecimal sumUkupnaCenaByFakturaId(@Param("fakturaId") Long fakturaId);

    @Query("""
            SELECT COALESCE(MAX(s.redniBroj), 0)
            FROM StavkaFakture s
            WHERE s.faktura.fakturaId = :fakturaId
            """)
    Integer maxRedniBrojByFakturaId(@Param("fakturaId") Long fakturaId);

    @Query("""
            SELECT COUNT(s)
            FROM StavkaFakture s
            WHERE s.faktura.fakturaId = :fakturaId
            """)
    long countByFakturaId(@Param("fakturaId") Long fakturaId);
}
