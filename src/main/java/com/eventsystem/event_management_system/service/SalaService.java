package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.SalaDto;
import com.eventsystem.event_management_system.dto.SalaUpdateDto;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.model.Sala;
import com.eventsystem.event_management_system.model.compositePK.SalaId;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import com.eventsystem.event_management_system.repository.SalaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalaService {

    private final SalaRepository salaRepository;
    private final LokacijaRepository lokacijaRepository;

    @Transactional(readOnly = true)
    public List<SalaDto> getSaleByLokacija(Long lokacijaId) {
        ensureLokacijaExists(lokacijaId);
        return salaRepository.findByLokacija_LokacijaId(lokacijaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalaDto getSala(Long lokacijaId, String nazivSale) {
        Sala sala = findSalaOrThrow(lokacijaId, nazivSale);
        return toDto(sala);
    }

    public SalaDto createSala(SalaDto dto) {
        Lokacija lokacija = lokacijaRepository.findById(dto.getLokacijaId())
                .orElseThrow(() -> new RuntimeException("Lokacija nije pronadjena sa id: " + dto.getLokacijaId()));

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
        return toDto(sala);
    }

    public SalaDto updateSala(Long lokacijaId, String nazivSale, SalaUpdateDto dto) {
        Sala existingSala = findSalaOrThrow(lokacijaId, nazivSale);

        existingSala.setTipSale(dto.getTipSale());
        existingSala.setKapacitet(dto.getKapacitet());
        existingSala.setBaznaCenaPoDanu(dto.getBaznaCenaPoDanu());

        salaRepository.save(existingSala);
        return toDto(existingSala);
    }

    public void deleteSala(Long lokacijaId, String nazivSale) {
        SalaId salaId = new SalaId(lokacijaId, nazivSale);
        if (!salaRepository.existsById(salaId)) {
            throw new RuntimeException("Sala nije pronadjena sa lokacijaId: " + lokacijaId + " i nazivom: " + nazivSale);
        }
        salaRepository.deleteById(salaId);
    }

    private Sala findSalaOrThrow(Long lokacijaId, String nazivSale) {
        SalaId salaId = new SalaId(lokacijaId, nazivSale);
        return salaRepository.findById(salaId)
                .orElseThrow(() -> new RuntimeException("Sala nije pronadjena sa lokacijaId: " + lokacijaId + " i nazivom: " + nazivSale));
    }

    private void ensureLokacijaExists(Long lokacijaId) {
        if (!lokacijaRepository.existsById(lokacijaId)) {
            throw new RuntimeException("Lokacija nije pronadjena sa id: " + lokacijaId);
        }
    }

    private SalaDto toDto(Sala sala) {
        return SalaDto.builder()
                .lokacijaId(sala.getId().getLokacijaId())
                .nazivSale(sala.getId().getNazivSale())
                .tipSale(sala.getTipSale())
                .kapacitet(sala.getKapacitet())
                .baznaCenaPoDanu(sala.getBaznaCenaPoDanu())
                .build();
    }
}
