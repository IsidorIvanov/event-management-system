package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.LokacijaDto;
import com.eventsystem.event_management_system.dto.LokacijaResponseDto;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LokacijaService {

    private final LokacijaRepository lokacijaRepository;

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
        lokacijaRepository.save(novaLokacija);
        return toResponseDto(novaLokacija);
    }

    public LokacijaResponseDto updateLokacija(Long id, LokacijaDto dto) {
        Lokacija l = lokacijaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lokacija not found with id: " + id));
        l.setNaziv(dto.getNaziv());
        l.setAdresa(dto.getAdresa());
        l.setGrad(dto.getGrad());
        l.setDrzava(dto.getDrzava());
        lokacijaRepository.save(l);
        return toResponseDto(l);
    }

    private LokacijaResponseDto toResponseDto(Lokacija l) {
        return new LokacijaResponseDto(l.getLokacijaId(), l.getNaziv(), l.getAdresa(), l.getGrad(), l.getDrzava());
    }

    public void deleteLokacija(Long id) {
        if (!lokacijaRepository.existsById(id)) {
            throw new RuntimeException("Lokacija not found with id: " + id);
        }
        lokacijaRepository.deleteById(id);
    }
}
