package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FakturaRepository extends JpaRepository<Faktura, Long> {
    Faktura findByBrojFakture(String brojFakture);

    @Query("select f from Faktura f where f.rokPlacanja < :d and f.status in :statuses")
    List<Faktura> findOverdue(@Param("d") LocalDate date, @Param("statuses") List<FakturaStatus> statuses);

    @Query("""
            SELECT COALESCE(SUM(f.placeniIznos), 0)
            FROM Faktura f
            WHERE f.dogadjajId = :dogadjajId
              AND f.tip = com.eventsystem.event_management_system.utils.enums.TipFakture.IZLAZNA
              AND f.status IN (
                  com.eventsystem.event_management_system.utils.enums.FakturaStatus.DELIMICNO_PLACENA,
                  com.eventsystem.event_management_system.utils.enums.FakturaStatus.PLACENA
              )
            """)
    BigDecimal sumNetoIzlazniPrihodByDogadjaj(@Param("dogadjajId") Long dogadjajId);

    @Query("""
            SELECT f
            FROM Faktura f
            WHERE f.tip = com.eventsystem.event_management_system.utils.enums.TipFakture.IZLAZNA
              AND f.klijentId = :klijentId
              AND f.datumIzdavanja BETWEEN :od AND :periodDo
            ORDER BY f.datumIzdavanja DESC
            """)
    List<Faktura> findIzlazneByKlijentAndPeriod(
            @Param("klijentId") Long klijentId,
            @Param("od") LocalDate od,
            @Param("periodDo") LocalDate periodDo
    );

    @Query("""
            SELECT COALESCE(SUM(f.ukupnaIznos), 0)
            FROM Faktura f
            WHERE f.tip = com.eventsystem.event_management_system.utils.enums.TipFakture.IZLAZNA
              AND f.klijentId = :klijentId
              AND f.datumIzdavanja BETWEEN :od AND :periodDo
            """)
    BigDecimal sumUkupnaIznosByKlijentAndPeriod(
            @Param("klijentId") Long klijentId,
            @Param("od") LocalDate od,
            @Param("periodDo") LocalDate periodDo
    );

    @Query("""
            SELECT COALESCE(SUM(f.placeniIznos), 0)
            FROM Faktura f
            WHERE f.tip = com.eventsystem.event_management_system.utils.enums.TipFakture.IZLAZNA
              AND f.klijentId = :klijentId
              AND f.datumIzdavanja BETWEEN :od AND :periodDo
            """)
    BigDecimal sumPlaceniIznosByKlijentAndPeriod(
            @Param("klijentId") Long klijentId,
            @Param("od") LocalDate od,
            @Param("periodDo") LocalDate periodDo
    );

    @Query("""
            SELECT f
            FROM Faktura f
            WHERE f.tip = com.eventsystem.event_management_system.utils.enums.TipFakture.ULAZNA
              AND f.dobavljacId = :dobavljacId
              AND f.datumIzdavanja BETWEEN :od AND :periodDo
            ORDER BY f.datumIzdavanja DESC
            """)
    List<Faktura> findUlazneByDobavljacAndPeriod(
            @Param("dobavljacId") Long dobavljacId,
            @Param("od") LocalDate od,
            @Param("periodDo") LocalDate periodDo
    );

    @Query("""
            SELECT COALESCE(SUM(f.placeniIznos), 0)
            FROM Faktura f
            WHERE f.tip = com.eventsystem.event_management_system.utils.enums.TipFakture.ULAZNA
              AND f.dobavljacId = :dobavljacId
              AND f.datumIzdavanja BETWEEN :od AND :periodDo
            """)
    BigDecimal sumPlaceniIznosUlazneByDobavljacAndPeriod(
            @Param("dobavljacId") Long dobavljacId,
            @Param("od") LocalDate od,
            @Param("periodDo") LocalDate periodDo
    );

    @Query("""
            SELECT COALESCE(SUM(f.placeniIznos), 0)
            FROM Faktura f
            WHERE f.tip = com.eventsystem.event_management_system.utils.enums.TipFakture.IZLAZNA
              AND f.status IN (
                  com.eventsystem.event_management_system.utils.enums.FakturaStatus.DELIMICNO_PLACENA,
                  com.eventsystem.event_management_system.utils.enums.FakturaStatus.PLACENA
              )
              AND f.datumIzdavanja BETWEEN :od AND :periodDo
            """)
    BigDecimal sumNetoIzlazniPrihodByPeriod(
            @Param("od") LocalDate od,
            @Param("periodDo") LocalDate periodDo
    );
}
