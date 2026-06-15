package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Notifikacija;
import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
import com.eventsystem.event_management_system.utils.enums.StatusNotifikacije;
import com.eventsystem.event_management_system.utils.enums.TipNotifikacije;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotifikacijaRepository extends JpaRepository<Notifikacija, Long> {

    /**
     * Stranica notifikacija za prikaz u aplikaciji (zvonce). Vraća samo PUSH kanal —
     * EMAIL notifikacije se isporučuju mejlom i ne prikazuju se in-app.
     * Redosled (npr. po vremenu slanja) dolazi iz {@link Pageable}.
     */
    @Query(value = "SELECT n FROM Notifikacija n LEFT JOIN FETCH n.dogadjaj " +
           "WHERE n.korisnik.korisnikId = :korisnikId " +
           "AND n.kanal = com.eventsystem.event_management_system.utils.enums.KanalNotifikacije.PUSH",
           countQuery = "SELECT COUNT(n) FROM Notifikacija n " +
           "WHERE n.korisnik.korisnikId = :korisnikId " +
           "AND n.kanal = com.eventsystem.event_management_system.utils.enums.KanalNotifikacije.PUSH")
    Page<Notifikacija> findMojeWithDogadjaj(@Param("korisnikId") Long korisnikId, Pageable pageable);

    /**
     * Kao {@link #findMojeWithDogadjaj}, ali ograničeno na zadati tip obaveštenja.
     */
    @Query(value = "SELECT n FROM Notifikacija n LEFT JOIN FETCH n.dogadjaj " +
           "WHERE n.korisnik.korisnikId = :korisnikId " +
           "AND n.kanal = com.eventsystem.event_management_system.utils.enums.KanalNotifikacije.PUSH " +
           "AND n.tip = :tip",
           countQuery = "SELECT COUNT(n) FROM Notifikacija n " +
           "WHERE n.korisnik.korisnikId = :korisnikId " +
           "AND n.kanal = com.eventsystem.event_management_system.utils.enums.KanalNotifikacije.PUSH " +
           "AND n.tip = :tip")
    Page<Notifikacija> findMojeWithDogadjajByTip(@Param("korisnikId") Long korisnikId,
                                                 @Param("tip") TipNotifikacije tip,
                                                 Pageable pageable);

    long countByKorisnikKorisnikIdAndKanalAndStatus(
            Long korisnikId, KanalNotifikacije kanal, StatusNotifikacije status);
}
