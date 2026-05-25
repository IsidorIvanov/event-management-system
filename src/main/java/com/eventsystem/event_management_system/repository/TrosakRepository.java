package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Trosak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TrosakRepository extends JpaRepository<Trosak, Long> {
    List<Trosak> findByFakturaIdAndTip(Long fakturaId, com.eventsystem.event_management_system.utils.enums.TipTroska tip);

    @Query("""
            SELECT COALESCE(SUM(t.iznos - COALESCE(t.refundiraniIznos, 0)), 0)
            FROM Trosak t
            WHERE t.budzetId = :budzetId AND t.kategorijaId = :kategorijaId
            """)
    BigDecimal sumNetoByBudzetAndKategorija(
            @Param("budzetId") Long budzetId,
            @Param("kategorijaId") Long kategorijaId
    );
}
