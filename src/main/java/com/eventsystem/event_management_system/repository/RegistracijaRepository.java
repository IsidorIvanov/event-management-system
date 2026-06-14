package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Registracija;
import com.eventsystem.event_management_system.utils.enums.StatusRegistracije;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistracijaRepository extends JpaRepository<Registracija, Long> {

    @Query("SELECT r FROM Registracija r JOIN FETCH r.tipKarte tk JOIN FETCH tk.dogadjaj d LEFT JOIN FETCH d.lokacija WHERE r.ucesnik.korisnikId = :korisnikId")
    List<Registracija> findByUcesnikIdWithDetails(@Param("korisnikId") Long korisnikId);

    @Query("SELECT r FROM Registracija r WHERE r.ucesnik.korisnikId = :korisnikId AND r.tipKarte.id.dogadjajId = :dogadjajId AND r.tipKarte.id.nazivTipa = :nazivTipa")
    Optional<Registracija> findByUcesnikAndTipKarte(
            @Param("korisnikId") Long korisnikId,
            @Param("dogadjajId") Long dogadjajId,
            @Param("nazivTipa") String nazivTipa);

    boolean existsByUcesnikKorisnikIdAndTipKarteIdDogadjajId(Long korisnikId, Long dogadjajId);

    boolean existsByUcesnikKorisnikIdAndTipKarteIdDogadjajIdAndStatusNot(Long korisnikId, Long dogadjajId, StatusRegistracije status);

    @Query("SELECT r FROM Registracija r JOIN FETCH r.ucesnik u JOIN FETCH r.tipKarte tk JOIN FETCH tk.dogadjaj d LEFT JOIN FETCH d.lokacija WHERE tk.id.dogadjajId = :dogadjajId")
    List<Registracija> findByDogadjajIdWithDetails(@Param("dogadjajId") Long dogadjajId);
}

