package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Sesija;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface SesijaRepository extends JpaRepository<Sesija, Long> {

    @Query("SELECT DISTINCT s FROM Sesija s LEFT JOIN FETCH s.govornici WHERE s.dogadjaj.dogadjajId = :dogadjajId")
    List<Sesija> findAllByDogadjaj_DogadjajId(@Param("dogadjajId") Long dogadjajId);

    @Query("SELECT s FROM Sesija s LEFT JOIN FETCH s.govornici WHERE s.sesijaId = :id")
    Optional<Sesija> findByIdWithGovornici(@Param("id") Long id);

    List<Sesija> findBySala_Lokacija_LokacijaIdAndDatumBetween(
            Long lokacijaId, LocalDate datumOd, LocalDate datumDo);

    /**
     * Sesije u istoj sali (lokacija + naziv sale) istog dana čija se satnica preklapa
     * sa zadatim terminom. Dva termina se preklapaju ako počinje pre nego što drugi
     * završi i obrnuto. {@code excludeId} (može biti null) izuzima sesiju koja se menja.
     */
    @Query("SELECT s FROM Sesija s " +
           "WHERE s.sala.id.lokacijaId = :lokacijaId AND s.sala.id.nazivSale = :nazivSale " +
           "AND s.datum = :datum " +
           "AND (:excludeId IS NULL OR s.sesijaId <> :excludeId) " +
           "AND s.vremePocetka < :vremeZavrsetka AND :vremePocetka < s.vremeZavrsetka")
    List<Sesija> findPreklapajuce(@Param("lokacijaId") Long lokacijaId,
                                  @Param("nazivSale") String nazivSale,
                                  @Param("datum") LocalDate datum,
                                  @Param("vremePocetka") LocalTime vremePocetka,
                                  @Param("vremeZavrsetka") LocalTime vremeZavrsetka,
                                  @Param("excludeId") Long excludeId);

    /** Sesije danas koje počinju u zadatom vremenskom prozoru a podsetnik još nije poslat (za P2). */
    @Query("SELECT s FROM Sesija s JOIN FETCH s.dogadjaj JOIN FETCH s.sala " +
           "WHERE s.datum = :datum AND s.vremePocetka >= :odVremena AND s.vremePocetka <= :doVremena " +
           "AND s.podsetnikPoslat = false")
    List<Sesija> findZaPodsetnik(@Param("datum") LocalDate datum,
                                 @Param("odVremena") LocalTime odVremena,
                                 @Param("doVremena") LocalTime doVremena);
}
