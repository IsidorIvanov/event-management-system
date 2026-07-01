package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.StavkaBudzeta;
import com.eventsystem.event_management_system.model.compositePK.StavkaBudzetaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface StavkaBudzetaRepository extends JpaRepository<StavkaBudzeta, StavkaBudzetaId> {

    List<StavkaBudzeta> findByBudzetBudzetId(Long budzetId);

    long countByBudzetBudzetId(Long budzetId);

    long countByKategorijaKategorijaId(Long kategorijaId);

    @Query("""
            SELECT COALESCE(SUM(s.planiraniIznos), 0)
            FROM StavkaBudzeta s
            WHERE s.budzet.budzetId = :budzetId
            """)
    BigDecimal sumPlaniraniByBudzetId(@Param("budzetId") Long budzetId);

    @Query("""
            SELECT COALESCE(SUM(s.stvarniIznos), 0)
            FROM StavkaBudzeta s
            WHERE s.budzet.budzetId = :budzetId
            """)
    BigDecimal sumStvarniByBudzetId(@Param("budzetId") Long budzetId);
}
