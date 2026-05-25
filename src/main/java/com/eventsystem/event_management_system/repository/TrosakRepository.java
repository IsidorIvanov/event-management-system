package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Trosak;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrosakRepository extends JpaRepository<Trosak, Long> {
	java.util.List<Trosak> findByFakturaIdAndTip(Long fakturaId, com.eventsystem.event_management_system.utils.enums.TipTroska tip);
}
