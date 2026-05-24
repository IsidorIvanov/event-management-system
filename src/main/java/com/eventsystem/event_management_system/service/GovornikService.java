package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.GovornikDto;
import com.eventsystem.event_management_system.model.Govornik;
import com.eventsystem.event_management_system.repository.GovornikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GovornikService {

    private final GovornikRepository govornikRepository;

    public GovornikDto createGovornik(GovornikDto dto) {
        if (dto.getEmail() != null && govornikRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Govornik sa emailom '" + dto.getEmail() + "' vec postoji.");
        }

        Govornik govornik = Govornik.builder()
                .ime(dto.getIme())
                .prezime(dto.getPrezime())
                .kompanija(dto.getKompanija())
                .pozicija(dto.getPozicija())
                .biografija(dto.getBiografija())
                .email(dto.getEmail())
                .honorar(dto.getHonorar())
                .build();

        govornikRepository.save(govornik);
        return dto;
    }

    public GovornikDto updateGovornik(Long id, GovornikDto dto) {
        Govornik existing = govornikRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Govornik not found with id: " + id));

        existing.setIme(dto.getIme());
        existing.setPrezime(dto.getPrezime());
        existing.setKompanija(dto.getKompanija());
        existing.setPozicija(dto.getPozicija());
        existing.setBiografija(dto.getBiografija());
        existing.setEmail(dto.getEmail());
        existing.setHonorar(dto.getHonorar());

        govornikRepository.save(existing);
        return dto;
    }

    public void deleteGovornik(Long id) {
        if (!govornikRepository.existsById(id)) {
            throw new RuntimeException("Govornik not found with id: " + id);
        }
        govornikRepository.deleteById(id);
    }
}
