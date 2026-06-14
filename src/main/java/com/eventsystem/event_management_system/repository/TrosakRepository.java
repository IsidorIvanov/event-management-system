package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Trosak;
import com.eventsystem.event_management_system.utils.enums.TipTroska;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TrosakRepository extends JpaRepository<Trosak, Long> {
    List<Trosak> findByFakturaIdAndTip(Long fakturaId, TipTroska tip);

    List<Trosak> findByBudzetIdAndKategorijaIdOrderByKreiranAtDesc(Long budzetId, Long kategorijaId);

    @Query("""
            SELECT t
            FROM Trosak t
            WHERE (:dogadjajId IS NULL OR t.dogadjajId = :dogadjajId)
              AND (:budzetId IS NULL OR t.budzetId = :budzetId)
              AND (:kategorijaId IS NULL OR t.kategorijaId = :kategorijaId)
              AND (:tip IS NULL OR t.tip = :tip)
            ORDER BY t.kreiranAt DESC
            """)
    List<Trosak> findAllFiltered(
            @Param("dogadjajId") Long dogadjajId,
            @Param("budzetId") Long budzetId,
            @Param("kategorijaId") Long kategorijaId,
            @Param("tip") TipTroska tip
    );

    @Query("""
            SELECT COALESCE(SUM(t.iznos - COALESCE(t.refundiraniIznos, 0)), 0)
            FROM Trosak t
            WHERE t.budzetId = :budzetId AND t.kategorijaId = :kategorijaId
            """)
    BigDecimal sumNetoByBudzetAndKategorija(
            @Param("budzetId") Long budzetId,
            @Param("kategorijaId") Long kategorijaId
    );

    @Query("""
            SELECT COALESCE(SUM(t.iznos - COALESCE(t.refundiraniIznos, 0)), 0)
            FROM Trosak t
            WHERE t.dogadjajId = :dogadjajId
            """)
    BigDecimal sumNetoByDogadjaj(@Param("dogadjajId") Long dogadjajId);
}
