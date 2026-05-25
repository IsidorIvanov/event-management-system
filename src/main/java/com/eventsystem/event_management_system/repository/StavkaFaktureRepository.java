package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.StavkaFakture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface StavkaFaktureRepository extends JpaRepository<StavkaFakture, Long> {

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
}
