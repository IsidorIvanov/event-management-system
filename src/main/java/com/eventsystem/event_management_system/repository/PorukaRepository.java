package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Poruka;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PorukaRepository extends JpaRepository<Poruka, Long> {

    @Query("SELECT p FROM Poruka p " +
           "JOIN FETCH p.posiljalac " +
           "JOIN FETCH p.primalac " +
           "WHERE (p.posiljalac.korisnikId = :user1 AND p.primalac.korisnikId = :user2) " +
           "   OR (p.posiljalac.korisnikId = :user2 AND p.primalac.korisnikId = :user1) " +
           "ORDER BY p.vremeSlamja ASC")
    List<Poruka> findKonverzacija(@Param("user1") Long user1, @Param("user2") Long user2);

    @Query("SELECT p FROM Poruka p " +
           "JOIN FETCH p.posiljalac " +
           "JOIN FETCH p.primalac " +
           "WHERE p.porukaId IN (" +
           "  SELECT MAX(p2.porukaId) FROM Poruka p2 " +
           "  WHERE p2.posiljalac.korisnikId = :korisnikId OR p2.primalac.korisnikId = :korisnikId " +
           "  GROUP BY CASE WHEN p2.posiljalac.korisnikId = :korisnikId THEN p2.primalac.korisnikId " +
           "               ELSE p2.posiljalac.korisnikId END" +
           ") ORDER BY p.vremeSlamja DESC")
    List<Poruka> findPoslednjePoruke(@Param("korisnikId") Long korisnikId);

    @Query("SELECT COUNT(p) FROM Poruka p WHERE p.primalac.korisnikId = :korisnikId AND p.posiljalac.korisnikId = :drugaStrana AND p.procitano = false")
    long countUnread(@Param("korisnikId") Long korisnikId, @Param("drugaStrana") Long drugaStrana);

    @Modifying
    @Query("UPDATE Poruka p SET p.procitano = true WHERE p.primalac.korisnikId = :korisnikId AND p.posiljalac.korisnikId = :drugaStrana AND p.procitano = false")
    void markAsRead(@Param("korisnikId") Long korisnikId, @Param("drugaStrana") Long drugaStrana);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Poruka p " +
           "WHERE (p.posiljalac.korisnikId = :user1 AND p.primalac.korisnikId = :user2) " +
           "   OR (p.posiljalac.korisnikId = :user2 AND p.primalac.korisnikId = :user1)")
    boolean existsKonverzacija(@Param("user1") Long user1, @Param("user2") Long user2);

    @Query("SELECT p FROM Poruka p JOIN FETCH p.posiljalac JOIN FETCH p.primalac " +
           "WHERE p.primalac.korisnikId = :korisnikId AND p.procitano = false")
    List<Poruka> findUnreadByRecipient(@Param("korisnikId") Long korisnikId);
}

