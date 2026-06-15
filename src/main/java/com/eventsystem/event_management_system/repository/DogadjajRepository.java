package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DogadjajRepository extends JpaRepository<Dogadjaj, Long> {

    @Query("SELECT DISTINCT d FROM Dogadjaj d JOIN FETCH d.lokacija LEFT JOIN FETCH d.tagovi")
    List<Dogadjaj> findAllWithLokacija();

    @Query("SELECT DISTINCT d FROM Dogadjaj d JOIN FETCH d.lokacija LEFT JOIN FETCH d.tagovi WHERE d.dogadjajId = :id")
    Optional<Dogadjaj> findByIdWithLokacija(@Param("id") Long id);

    /** Događaji čiji je datum početka današnji dan, a još su u statusu OBJAVLJEN (za D6). */
    @Query("SELECT d FROM Dogadjaj d WHERE d.status = :status AND d.datumPocetka = :danas")
    List<Dogadjaj> findZaAktivaciju(@Param("status") StatusDogadjaja status,
                                    @Param("danas") LocalDate danas);
}