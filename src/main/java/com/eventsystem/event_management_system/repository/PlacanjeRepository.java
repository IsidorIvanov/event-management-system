package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Placanje;
import com.eventsystem.event_management_system.utils.enums.PlacanjeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface PlacanjeRepository extends JpaRepository<Placanje, Long> {

    @Query("""
            SELECT COALESCE(SUM(p.iznos), 0)
            FROM Placanje p
            WHERE p.faktura.fakturaId = :fakturaId
              AND p.status = :status
            """)
    BigDecimal sumByFakturaIdAndStatus(
            @Param("fakturaId") Long fakturaId,
            @Param("status") PlacanjeStatus status
    );
}
