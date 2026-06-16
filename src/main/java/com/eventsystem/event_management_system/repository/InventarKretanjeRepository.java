package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.InventarKretanje;
import com.eventsystem.event_management_system.utils.enums.TipInventarKretanja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventarKretanjeRepository extends JpaRepository<InventarKretanje, Long> {

    boolean existsByPorudzbenicaPorudzbenicaIdAndTip(Long porudzbenicaId, TipInventarKretanja tip);

    List<InventarKretanje> findByOpremaOpremaIdOrderByKreiranAtDesc(Long opremaId);

    List<InventarKretanje> findByPorudzbenicaPorudzbenicaId(Long porudzbenicaId);
}
