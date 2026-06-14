package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.UcesnikSesija;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UcesnikSesijaRepository extends JpaRepository<UcesnikSesija, Long> {

    @Query("SELECT us FROM UcesnikSesija us " +
           "LEFT JOIN FETCH us.sesija s " +
           "LEFT JOIN FETCH s.dogadjaj d " +
           "LEFT JOIN FETCH s.sala sala " +
           "LEFT JOIN FETCH sala.lokacija l " +
           "LEFT JOIN FETCH s.govornici " +
           "WHERE us.ucesnik.korisnikId = :ucesnikId")
    List<UcesnikSesija> findByUcesnikIdWithDetails(@Param("ucesnikId") Long ucesnikId);

    Optional<UcesnikSesija> findByUcesnik_KorisnikIdAndSesija_SesijaId(Long ucesnikId, Long sesijaId);

    boolean existsByUcesnik_KorisnikIdAndSesija_SesijaId(Long ucesnikId, Long sesijaId);

    void deleteByUcesnik_KorisnikIdAndSesija_SesijaId(Long ucesnikId, Long sesijaId);
}

