package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Notifikacija;
import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
import com.eventsystem.event_management_system.utils.enums.StatusNotifikacije;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotifikacijaRepository extends JpaRepository<Notifikacija, Long> {

    /**
     * Notifikacije za prikaz u aplikaciji (zvonce). Vraća samo PUSH kanal —
     * EMAIL notifikacije se isporučuju mejlom i ne prikazuju se in-app.
     */
    @Query("SELECT n FROM Notifikacija n LEFT JOIN FETCH n.dogadjaj " +
           "WHERE n.korisnik.korisnikId = :korisnikId " +
           "AND n.kanal = com.eventsystem.event_management_system.utils.enums.KanalNotifikacije.PUSH " +
           "ORDER BY n.vremeSlanja DESC")
    List<Notifikacija> findMojeWithDogadjaj(@Param("korisnikId") Long korisnikId);

    long countByKorisnikKorisnikIdAndKanalAndStatus(
            Long korisnikId, KanalNotifikacije kanal, StatusNotifikacije status);
}
