package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Nabavka;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NabavkaRepository extends JpaRepository<Nabavka, Long> {

    List<Nabavka> findByDogadjajDogadjajIdOrderByKreiranAtDesc(Long dogadjajId);

    @Query("""
            SELECT n FROM Nabavka n
            JOIN FETCH n.dogadjaj
            JOIN FETCH n.kreirao
            LEFT JOIN FETCH n.dobavljac
            LEFT JOIN FETCH n.stavke
            WHERE n.nabavkaId = :id
            """)
    Optional<Nabavka> findByIdWithDetalji(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT n FROM Nabavka n
            JOIN FETCH n.dogadjaj
            JOIN FETCH n.kreirao
            LEFT JOIN FETCH n.dobavljac
            ORDER BY n.kreiranAt DESC
            """)
    List<Nabavka> findAllWithDetalji();

    boolean existsByDobavljacDobavljacIdAndStatusNotIn(Long dobavljacId, java.util.Collection<StatusNabavke> statuses);
}
