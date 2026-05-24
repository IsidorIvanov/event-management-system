package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Sesija;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SesijaRepository extends JpaRepository<Sesija, Long> {

    @Query("SELECT DISTINCT s FROM Sesija s LEFT JOIN FETCH s.govornici WHERE s.dogadjaj.dogadjajId = :dogadjajId")
    List<Sesija> findAllByDogadjaj_DogadjajId(@Param("dogadjajId") Long dogadjajId);

    @Query("SELECT s FROM Sesija s LEFT JOIN FETCH s.govornici WHERE s.sesijaId = :id")
    Optional<Sesija> findByIdWithGovornici(@Param("id") Long id);

    List<Sesija> findBySala_Lokacija_LokacijaIdAndDatumBetween(
            Long lokacijaId, LocalDate datumOd, LocalDate datumDo);
}
