package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Cenovnik;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CenovnikRepository extends JpaRepository<Cenovnik, Long> {

    List<Cenovnik> findByDobavljacDobavljacId(Long dobavljacId);

    @Query("""
            SELECT c FROM Cenovnik c
            JOIN FETCH c.dobavljac d
            WHERE (c.dostupnost = true OR c.dostupnost IS NULL)
              AND d.status = com.eventsystem.event_management_system.utils.enums.StatusDobavljaca.AKTIVAN
              AND LOWER(c.nazivResursa) = LOWER(:nazivResursa)
            """)
    List<Cenovnik> findDostupnePoNazivuResursa(@Param("nazivResursa") String nazivResursa);

    @Query("""
            SELECT c FROM Cenovnik c
            JOIN FETCH c.dobavljac d
            WHERE (c.dostupnost = true OR c.dostupnost IS NULL)
              AND d.status = com.eventsystem.event_management_system.utils.enums.StatusDobavljaca.AKTIVAN
            """)
    List<Cenovnik> findSveDostupne();
}
