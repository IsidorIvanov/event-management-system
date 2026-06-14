package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.utils.enums.UlogaZaposlenog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZaposleniRepository extends JpaRepository<Zaposleni, Long> {
    List<Zaposleni> findByUloga(UlogaZaposlenog uloga);
    Optional<Zaposleni> findByEmail(String email);
}

