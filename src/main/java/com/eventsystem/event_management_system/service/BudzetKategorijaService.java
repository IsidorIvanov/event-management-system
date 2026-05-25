package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.BudzetKategorijaDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.BudzetKategorija;
import com.eventsystem.event_management_system.repository.BudzetKategorijaRepository;
import com.eventsystem.event_management_system.repository.StavkaBudzetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudzetKategorijaService {

    private final BudzetKategorijaRepository kategorijaRepository;
    private final StavkaBudzetaRepository stavkaBudzetaRepository;

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public List<BudzetKategorijaDto> getAll() {
        return kategorijaRepository.findAll().stream()
                .sorted(Comparator.comparing(BudzetKategorija::getNaziv, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public BudzetKategorijaDto getById(Long id) {
        return toDto(findEntity(id));
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public BudzetKategorijaDto create(BudzetKategorijaDto dto) {
        String naziv = normalizeNaziv(dto.getNaziv());
        if (kategorijaRepository.existsByNazivIgnoreCase(naziv)) {
            throw new BadRequestException("Kategorija budžeta sa ovim nazivom već postoji.");
        }

        BudzetKategorija saved = kategorijaRepository.save(BudzetKategorija.builder()
                .naziv(naziv)
                .opis(dto.getOpis())
                .build());
        return toDto(saved);
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public BudzetKategorijaDto update(Long id, BudzetKategorijaDto dto) {
        BudzetKategorija kategorija = findEntity(id);
        String naziv = normalizeNaziv(dto.getNaziv());

        kategorijaRepository.findByNazivIgnoreCase(naziv)
                .filter(existing -> !existing.getKategorijaId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("Kategorija budžeta sa ovim nazivom već postoji.");
                });

        kategorija.setNaziv(naziv);
        kategorija.setOpis(dto.getOpis());
        return toDto(kategorijaRepository.save(kategorija));
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public void delete(Long id) {
        BudzetKategorija kategorija = findEntity(id);
        if (stavkaBudzetaRepository.countByKategorijaKategorijaId(id) > 0) {
            throw new BadRequestException("Kategorija se ne može obrisati jer je povezana sa stavkama budžeta.");
        }
        kategorijaRepository.delete(kategorija);
    }

    public BudzetKategorija findEntity(Long id) {
        return kategorijaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kategorija budžeta nije pronađena sa id: " + id));
    }

    private String normalizeNaziv(String naziv) {
        if (naziv == null || naziv.trim().isEmpty()) {
            throw new BadRequestException("Naziv kategorije je obavezan.");
        }
        return naziv.trim();
    }

    private BudzetKategorijaDto toDto(BudzetKategorija kategorija) {
        return BudzetKategorijaDto.builder()
                .kategorijaId(kategorija.getKategorijaId())
                .naziv(kategorija.getNaziv())
                .opis(kategorija.getOpis())
                .build();
    }
}
