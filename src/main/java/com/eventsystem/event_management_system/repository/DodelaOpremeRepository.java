package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.DodelaOpreme;
import com.eventsystem.event_management_system.utils.enums.StatusDodeleOpreme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DodelaOpremeRepository extends JpaRepository<DodelaOpreme, Long> {

    List<DodelaOpreme> findByDogadjajDogadjajId(Long dogadjajId);

    List<DodelaOpreme> findBySesijaSesijaId(Long sesijaId);

    @Query("""
            SELECT COALESCE(SUM(d.kolicina), 0) FROM DodelaOpreme d
            WHERE d.oprema.opremaId = :opremaId
              AND d.status IN :statusi
            """)
    int sumAktivnaKolicina(@Param("opremaId") Long opremaId,
                           @Param("statusi") List<StatusDodeleOpreme> statusi);

    @Query("""
            SELECT d FROM DodelaOpreme d
            JOIN FETCH d.oprema
            LEFT JOIN FETCH d.sesija
            WHERE d.dogadjaj.dogadjajId = :dogadjajId
            ORDER BY d.datumOd
            """)
    List<DodelaOpreme> findByDogadjajWithDetalji(@Param("dogadjajId") Long dogadjajId);

    List<DodelaOpreme> findByStatusInAndDatumDoBefore(List<StatusDodeleOpreme> statusi, LocalDate datum);
}
