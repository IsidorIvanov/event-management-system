package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.CenovnikDto;
import com.eventsystem.event_management_system.model.Cenovnik;
import com.eventsystem.event_management_system.repository.CenovnikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CenovnikService {

    private final CenovnikRepository cenovnikRepository;
    private final DobavljacService dobavljacService;

    @Transactional(readOnly = true)
    public List<CenovnikDto> getAll() {
        return cenovnikRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CenovnikDto> getByDobavljac(Long dobavljacId) {
        return cenovnikRepository.findByDobavljacDobavljacId(dobavljacId).stream()
                .map(this::toDto)
                .toList();
    }

    public CenovnikDto create(CenovnikDto dto) {
        Cenovnik entity = Cenovnik.builder()
                .dobavljac(dobavljacService.findEntity(dto.getDobavljacId()))
                .nazivResursa(dto.getNazivResursa())
                .opis(dto.getOpis())
                .jedinicaMere(dto.getJedinicaMere())
                .cenaJedinicna(dto.getCenaJedinicna())
                .dostupnost(dto.getDostupnost() != null ? dto.getDostupnost() : true)
                .build();
        return toDto(cenovnikRepository.save(entity));
    }

    public CenovnikDto update(Long id, CenovnikDto dto) {
        Cenovnik entity = cenovnikRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stavka cenovnika nije pronadjena sa id: " + id));
        entity.setNazivResursa(dto.getNazivResursa());
        entity.setOpis(dto.getOpis());
        entity.setJedinicaMere(dto.getJedinicaMere());
        entity.setCenaJedinicna(dto.getCenaJedinicna());
        if (dto.getDostupnost() != null) {
            entity.setDostupnost(dto.getDostupnost());
        }
        return toDto(cenovnikRepository.save(entity));
    }

    public void delete(Long id) {
        if (!cenovnikRepository.existsById(id)) {
            throw new RuntimeException("Stavka cenovnika nije pronadjena sa id: " + id);
        }
        cenovnikRepository.deleteById(id);
    }

    public Cenovnik findEntity(Long id) {
        return cenovnikRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stavka cenovnika nije pronadjena sa id: " + id));
    }

    private CenovnikDto toDto(Cenovnik entity) {
        return CenovnikDto.builder()
                .cenovnikId(entity.getCenovnikId())
                .dobavljacId(entity.getDobavljac().getDobavljacId())
                .dobavljacNaziv(entity.getDobavljac().getNaziv())
                .nazivResursa(entity.getNazivResursa())
                .opis(entity.getOpis())
                .jedinicaMere(entity.getJedinicaMere())
                .cenaJedinicna(entity.getCenaJedinicna())
                .dostupnost(entity.getDostupnost())
                .build();
    }
}
