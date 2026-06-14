package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.AnalizaProfitabilnosti;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnalizaProfitabilnostiRepository extends JpaRepository<AnalizaProfitabilnosti, Long> {

    @Query("""
            SELECT a
            FROM AnalizaProfitabilnosti a
            JOIN FETCH a.dogadjaj d
            JOIN FETCH a.kreirao k
            WHERE a.analizaId = :analizaId
            """)
    Optional<AnalizaProfitabilnosti> findByIdWithDetalji(@Param("analizaId") Long analizaId);

    @Query("""
            SELECT a
            FROM AnalizaProfitabilnosti a
            JOIN FETCH a.dogadjaj d
            JOIN FETCH a.kreirao k
            WHERE d.dogadjajId = :dogadjajId
            ORDER BY a.datumAnalize DESC, a.analizaId DESC
            """)
    List<AnalizaProfitabilnosti> findByDogadjajDogadjajIdOrderByDatumAnalizeDesc(@Param("dogadjajId") Long dogadjajId);
}
