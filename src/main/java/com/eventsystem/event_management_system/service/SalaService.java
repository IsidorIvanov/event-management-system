package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.SalaDto;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.model.Sala;
import com.eventsystem.event_management_system.model.compositePK.SalaId;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import com.eventsystem.event_management_system.repository.SalaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SalaService {

    private final SalaRepository salaRepository;
    private final LokacijaRepository lokacijaRepository;

    public SalaDto createSala(SalaDto dto) {
        Lokacija lokacija = lokacijaRepository.findById(dto.getLokacijaId())
                .orElseThrow(() -> new RuntimeException("Lokacija not found with id: " + dto.getLokacijaId()));

        SalaId salaId = new SalaId(dto.getLokacijaId(), dto.getNazivSale());

        if (salaRepository.existsById(salaId)) {
            throw new RuntimeException("Sala sa nazivom '" + dto.getNazivSale() + "' vec postoji na ovoj lokaciji.");
        }

        Sala sala = Sala.builder()
                .id(salaId)
                .lokacija(lokacija)
                .tipSale(dto.getTipSale())
                .kapacitet(dto.getKapacitet())
                .baznaCenaPoDanu(dto.getBaznaCenaPoDanu())
                .build();

        salaRepository.save(sala);
        return dto;
    }

    public SalaDto updateSala(Long lokacijaId, String nazivSale, SalaDto dto) {
        SalaId salaId = new SalaId(lokacijaId, nazivSale);
        Sala existingSala = salaRepository.findById(salaId)
                .orElseThrow(() -> new RuntimeException("Sala not found with lokacijaId: " + lokacijaId + " and naziv: " + nazivSale));

        existingSala.setTipSale(dto.getTipSale());
        existingSala.setKapacitet(dto.getKapacitet());
        existingSala.setBaznaCenaPoDanu(dto.getBaznaCenaPoDanu());

        salaRepository.save(existingSala);
        return dto;
    }

    public void deleteSala(Long lokacijaId, String nazivSale) {
        SalaId salaId = new SalaId(lokacijaId, nazivSale);
        if (!salaRepository.existsById(salaId)) {
            throw new RuntimeException("Sala not found with lokacijaId: " + lokacijaId + " and naziv: " + nazivSale);
        }
        salaRepository.deleteById(salaId);
    }
}
