package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.StavkaNabavke;
import com.eventsystem.event_management_system.model.compositePK.StavkaNabavkeId;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface StavkaNabavkeRepository extends JpaRepository<StavkaNabavke, StavkaNabavkeId> {

    List<StavkaNabavke> findByNabavkaNabavkaIdOrderByIdRedniBrojAsc(Long nabavkaId);

    @Query("""
            SELECT COALESCE(SUM(s.ukupnaCena), 0)
            FROM StavkaNabavke s
            WHERE s.nabavka.dogadjaj.dogadjajId = :dogadjajId
              AND s.nabavka.status IN :statuses
            """)
    BigDecimal sumTrosakStavkiNabavke(
            @Param("dogadjajId") Long dogadjajId,
            @Param("statuses") Collection<StatusNabavke> statuses
    );

    @Query("""
            SELECT COALESCE(SUM(s.kolicina), 0)
            FROM StavkaNabavke s
            WHERE (s.cenovnik.cenovnikId = :cenovnikId
                   OR LOWER(TRIM(s.nazivResursa)) = LOWER(TRIM(:nazivResursa)))
              AND s.nabavka.status IN :statuses
            """)
    int sumPotraznjaKolicina(
            @Param("cenovnikId") Long cenovnikId,
            @Param("nazivResursa") String nazivResursa,
            @Param("statuses") Collection<StatusNabavke> statuses
    );

    @Query("""
            SELECT COUNT(DISTINCT s.nabavka.dogadjaj.dogadjajId)
            FROM StavkaNabavke s
            WHERE (s.cenovnik.cenovnikId = :cenovnikId
                   OR LOWER(TRIM(s.nazivResursa)) = LOWER(TRIM(:nazivResursa)))
              AND s.nabavka.status IN :statuses
            """)
    int countDogadjajiSaPotrebom(
            @Param("cenovnikId") Long cenovnikId,
            @Param("nazivResursa") String nazivResursa,
            @Param("statuses") Collection<StatusNabavke> statuses
    );
}
