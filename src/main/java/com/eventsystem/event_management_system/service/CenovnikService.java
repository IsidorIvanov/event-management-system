package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.CenovnikDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Cenovnik;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.repository.CenovnikRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDobavljaca;
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
    public CenovnikDto getById(Long id) {
        return toDto(findEntity(id));
    }

    @Transactional(readOnly = true)
    public List<CenovnikDto> getByDobavljac(Long dobavljacId) {
        return cenovnikRepository.findByDobavljacDobavljacId(dobavljacId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CenovnikDto create(CenovnikDto dto) {
        Dobavljac dobavljac = resolveDobavljacForCenovnik(dto.getDobavljacId());
        Cenovnik entity = Cenovnik.builder()
                .dobavljac(dobavljac)
                .nazivResursa(normalizeRequired(dto.getNazivResursa(), "Naziv resursa"))
                .opis(dto.getOpis())
                .jedinicaMere(normalizeRequired(dto.getJedinicaMere(), "Jedinica mere"))
                .cenaJedinicna(dto.getCenaJedinicna())
                .dostupnost(dto.getDostupnost() != null ? dto.getDostupnost() : true)
                .build();
        return toDto(cenovnikRepository.save(entity));
    }

    @Transactional
    public CenovnikDto update(Long id, CenovnikDto dto) {
        Cenovnik entity = findEntity(id);
        if (dto.getDobavljacId() != null && !dto.getDobavljacId().equals(entity.getDobavljac().getDobavljacId())) {
            entity.setDobavljac(resolveDobavljacForCenovnik(dto.getDobavljacId()));
        }
        entity.setNazivResursa(normalizeRequired(dto.getNazivResursa(), "Naziv resursa"));
        entity.setOpis(dto.getOpis());
        entity.setJedinicaMere(normalizeRequired(dto.getJedinicaMere(), "Jedinica mere"));
        entity.setCenaJedinicna(dto.getCenaJedinicna());
        if (dto.getDostupnost() != null) {
            entity.setDostupnost(dto.getDostupnost());
        }
        return toDto(cenovnikRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!cenovnikRepository.existsById(id)) {
            throw new NotFoundException("Stavka cenovnika nije pronadjena sa id: " + id);
        }
        cenovnikRepository.deleteById(id);
    }

    public Cenovnik findEntity(Long id) {
        return cenovnikRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stavka cenovnika nije pronadjena sa id: " + id));
    }

    private Dobavljac resolveDobavljacForCenovnik(Long dobavljacId) {
        if (dobavljacId == null) {
            throw new BadRequestException("Dobavljač je obavezan.");
        }
        Dobavljac dobavljac = dobavljacService.findEntity(dobavljacId);
        if (dobavljac.getStatus() == StatusDobavljaca.SUSPENDOVAN) {
            throw new BadRequestException("Ne možete dodati stavku cenovnika suspendovanom dobavljaču.");
        }
        return dobavljac;
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " je obavezan.");
        }
        return value.trim();
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
