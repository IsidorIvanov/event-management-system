package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.LokacijaDto;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LokacijaService {

    private final LokacijaRepository lokacijaRepository;

    public LokacijaDto saveLokacija(LokacijaDto dto) {
        Lokacija novaLokacija = Lokacija.builder()
                .naziv(dto.getNaziv())
                .adresa(dto.getAdresa())
                .grad(dto.getGrad())
                .drzava(dto.getDrzava())
                .build();

        lokacijaRepository.save(novaLokacija);

        return dto;
    }

    public LokacijaDto updateLokacija(Long id, LokacijaDto dto) {
        Lokacija existingLokacija = lokacijaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lokacija not found with id: " + id));

        existingLokacija.setNaziv(dto.getNaziv());
        existingLokacija.setAdresa(dto.getAdresa());
        existingLokacija.setGrad(dto.getGrad());
        existingLokacija.setDrzava(dto.getDrzava());

        lokacijaRepository.save(existingLokacija);

        return dto;
    }

    public void deleteLokacija(Long id) {
        if (!lokacijaRepository.existsById(id)) {
            throw new RuntimeException("Lokacija not found with id: " + id);
        }
        lokacijaRepository.deleteById(id);
    }
}
