package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.PlacanjeDto;
import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.model.Placanje;
import com.eventsystem.event_management_system.repository.PlacanjeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlacanjeService {

    private final PlacanjeRepository placanjeRepository;
    private final FakturaService fakturaService;

    @Transactional
    public Placanje create(Long fakturaId, PlacanjeDto dto) {
        Faktura f = fakturaService.findEntity(fakturaId);
        Placanje p = Placanje.builder()
                .faktura(f)
                .iznos(dto.getIznos())
                .metod(dto.getMetod())
                .status(com.eventsystem.event_management_system.utils.enums.PlacanjeStatus.PENDING)
                .kreiranoAt(LocalDateTime.now())
                .build();
        p = placanjeRepository.save(p);
        f.getPlacanja().add(p);
        return p;
    }

    @Transactional
    public Placanje confirm(Long placanjeId) {
        Placanje p = placanjeRepository.findById(placanjeId).orElseThrow(() -> new RuntimeException("Placanje not found"));
        p.setStatus(com.eventsystem.event_management_system.utils.enums.PlacanjeStatus.COMPLETED);
        p.setPotvrdjenoAt(LocalDateTime.now());
        placanjeRepository.save(p);

        // algoritam 8.2
        fakturaService.recomputePlaceniIznos(p.getFaktura());
        return p;
    }

    @Transactional
    public Placanje fail(Long placanjeId) {
        Placanje p = placanjeRepository.findById(placanjeId).orElseThrow(() -> new RuntimeException("Placanje not found"));
        p.setStatus(com.eventsystem.event_management_system.utils.enums.PlacanjeStatus.FAILED);
        return placanjeRepository.save(p);
    }
}
