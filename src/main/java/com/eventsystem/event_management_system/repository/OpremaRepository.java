package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Oprema;
import com.eventsystem.event_management_system.utils.enums.StatusOpreme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OpremaRepository extends JpaRepository<Oprema, Long> {

    List<Oprema> findByStatus(StatusOpreme status);

    List<Oprema> findByKategorijaIgnoreCase(String kategorija);

    Optional<Oprema> findFirstByNazivIgnoreCase(String naziv);
}
