package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.LokacijaDto;

import com.eventsystem.event_management_system.dto.SalaDto;
import com.eventsystem.event_management_system.dto.LokacijaResponseDto;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.model.Sala;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import com.eventsystem.event_management_system.repository.SalaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LokacijaService {

    private final LokacijaRepository lokacijaRepository;
    private final SalaRepository salaRepository;

    @Transactional(readOnly = true)
    public List<LokacijaDto> getAllLokacije() {
        return lokacijaRepository.findAll().stream()
                .map(this::toDtoWithSale)
                .toList();
    }

    @Transactional(readOnly = true)
    public LokacijaDto getLokacijaById(Long id) {
        Lokacija lokacija = lokacijaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lokacija nije pronadjena sa id: " + id));
        return toDtoWithSale(lokacija);
    }

    public List<LokacijaResponseDto> getAllLokacije() {
        return lokacijaRepository.findAll().stream()
                .map(l -> new LokacijaResponseDto(l.getLokacijaId(), l.getNaziv(), l.getAdresa(), l.getGrad(), l.getDrzava()))
                .collect(Collectors.toList());
    }

    public LokacijaResponseDto saveLokacija(LokacijaDto dto) {
        Lokacija novaLokacija = Lokacija.builder()
                .naziv(dto.getNaziv())
                .adresa(dto.getAdresa())
                .grad(dto.getGrad())
                .drzava(dto.getDrzava())
                .build();

        Lokacija saved = lokacijaRepository.save(novaLokacija);
        return toDto(saved);
    }

    public LokacijaDto updateLokacija(Long id, LokacijaDto dto) {
        Lokacija existingLokacija = lokacijaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lokacija nije pronadjena sa id: " + id));

        existingLokacija.setNaziv(dto.getNaziv());
        existingLokacija.setAdresa(dto.getAdresa());
        existingLokacija.setGrad(dto.getGrad());
        existingLokacija.setDrzava(dto.getDrzava());

        Lokacija saved = lokacijaRepository.save(existingLokacija);
        return toDto(saved);
    }

    public void deleteLokacija(Long id) {
        if (!lokacijaRepository.existsById(id)) {
            throw new RuntimeException("Lokacija nije pronadjena sa id: " + id);
        }
        lokacijaRepository.deleteById(id);
    }

    private LokacijaDto toDto(Lokacija lokacija) {
        return LokacijaDto.builder()
                .lokacijaId(lokacija.getLokacijaId())
                .naziv(lokacija.getNaziv())
                .adresa(lokacija.getAdresa())
                .grad(lokacija.getGrad())
                .drzava(lokacija.getDrzava())
                .build();
    }

    private LokacijaDto toDtoWithSale(Lokacija lokacija) {
        LokacijaDto dto = toDto(lokacija);
        List<Sala> sale = salaRepository.findByLokacija_LokacijaId(lokacija.getLokacijaId());
        dto.setSale(sale.stream().map(this::toSalaDto).toList());
        return dto;
    }

    private SalaDto toSalaDto(Sala sala) {
        return SalaDto.builder()
                .lokacijaId(sala.getId().getLokacijaId())
                .nazivSale(sala.getId().getNazivSale())
                .tipSale(sala.getTipSale())
                .kapacitet(sala.getKapacitet())
                .baznaCenaPoDanu(sala.getBaznaCenaPoDanu())
                .build();
    }
}
