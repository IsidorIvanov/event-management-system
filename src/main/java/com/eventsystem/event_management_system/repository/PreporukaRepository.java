package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Preporuka;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreporukaRepository extends JpaRepository<Preporuka, Long> {

    @Modifying
    void deleteByUcesnik_KorisnikId(Long korisnikId);

    @Query("""
            SELECT p FROM Preporuka p
            JOIN FETCH p.dogadjaj d
            JOIN FETCH d.lokacija
            LEFT JOIN FETCH d.tagovi
            WHERE p.ucesnik.korisnikId = :korisnikId
            """)
    List<Preporuka> findByUcesnikIdWithDetails(@Param("korisnikId") Long korisnikId);
}
