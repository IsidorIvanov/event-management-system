package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FakturaRepository extends JpaRepository<Faktura, Long> {
    Faktura findByBrojFakture(String brojFakture);

    @Query("select f from Faktura f where f.rokPlacanja < :d and f.status in :statuses")
    List<Faktura> findOverdue(@Param("d") LocalDate date, @Param("statuses") List<FakturaStatus> statuses);
}
