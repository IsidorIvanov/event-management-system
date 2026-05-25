package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Refundacija;
import com.eventsystem.event_management_system.utils.enums.RefundacijaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;

public interface RefundacijaRepository extends JpaRepository<Refundacija, Long> {

    @Query("""
            SELECT COALESCE(SUM(r.iznos), 0)
            FROM Refundacija r
            WHERE r.placanje.placanjeId = :placanjeId
              AND r.status IN :statuses
            """)
    BigDecimal sumByPlacanjeIdAndStatuses(
            @Param("placanjeId") Long placanjeId,
            @Param("statuses") Collection<RefundacijaStatus> statuses
    );

    @Query("""
            SELECT COALESCE(SUM(r.iznos), 0)
            FROM Refundacija r
            WHERE r.placanje.faktura.fakturaId = :fakturaId
              AND r.status = :status
            """)
    BigDecimal sumByFakturaIdAndStatus(
            @Param("fakturaId") Long fakturaId,
            @Param("status") RefundacijaStatus status
    );
}
