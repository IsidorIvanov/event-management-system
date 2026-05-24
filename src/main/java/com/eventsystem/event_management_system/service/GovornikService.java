package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.GovornikDto;
import com.eventsystem.event_management_system.model.Govornik;
import com.eventsystem.event_management_system.repository.GovornikRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GovornikService {

    private final GovornikRepository govornikRepository;
    private final SesijaRepository sesijaRepository;

    @Transactional(readOnly = true)
    public List<GovornikDto> getAllGovornici() {
        return govornikRepository.findAllWithSesije().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GovornikDto> getGovornikByDogadjaj(Long dogadjajId) {
        return govornikRepository.findDistinctBySesijeDogadjajDogadjajId(dogadjajId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
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
        return toDto(govornik);
    }

    @Transactional
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
        return toDto(existing);
    }

    @Transactional
    public void deleteGovornik(Long id) {
        if (!govornikRepository.existsById(id)) {
            throw new RuntimeException("Govornik not found with id: " + id);
        }
        govornikRepository.deleteById(id);
    }

    private GovornikDto toDto(Govornik g) {
        List<Long> sesijaIds = g.getSesije() == null ? List.of() :
                g.getSesije().stream().map(s -> s.getSesijaId()).collect(Collectors.toList());
        return GovornikDto.builder()
                .govornikId(g.getGovornikId())
                .ime(g.getIme())
                .prezime(g.getPrezime())
                .kompanija(g.getKompanija())
                .pozicija(g.getPozicija())
                .biografija(g.getBiografija())
                .email(g.getEmail())
                .honorar(g.getHonorar())
                .sesijaIds(sesijaIds)
                .build();
    }
}
