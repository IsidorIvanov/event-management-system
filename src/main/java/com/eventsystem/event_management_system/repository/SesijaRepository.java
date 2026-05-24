package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Sesija;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SesijaRepository extends JpaRepository<Sesija, Long> {

    List<Sesija> findAllByDogadjaj_DogadjajId(Long dogadjajId);

    List<Sesija> findBySala_Lokacija_LokacijaIdAndDatumBetween(
            Long lokacijaId, LocalDate datumOd, LocalDate datumDo);
}
