package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DobavljacDto;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.repository.DobavljacRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDobavljaca;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DobavljacService {

    private final DobavljacRepository dobavljacRepository;

    @Transactional(readOnly = true)
    public List<DobavljacDto> getAll() {
        return dobavljacRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<DobavljacDto> getAktivni() {
        return dobavljacRepository.findByStatus(StatusDobavljaca.AKTIVAN).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DobavljacDto getById(Long id) {
        return toDto(findEntity(id));
    }

    public DobavljacDto create(DobavljacDto dto) {
        Dobavljac entity = Dobavljac.builder()
                .naziv(dto.getNaziv())
                .grad(dto.getGrad())
                .kontaktEmail(dto.getKontaktEmail())
                .telefon(dto.getTelefon())
                .pib(dto.getPib())
                .status(dto.getStatus() != null ? dto.getStatus() : StatusDobavljaca.AKTIVAN)
                .rejting(dto.getRejting())
                .kriterijumi(dto.getKriterijumi())
                .build();
        return toDto(dobavljacRepository.save(entity));
    }

    public DobavljacDto update(Long id, DobavljacDto dto) {
        Dobavljac entity = findEntity(id);
        entity.setNaziv(dto.getNaziv());
        entity.setGrad(dto.getGrad());
        entity.setKontaktEmail(dto.getKontaktEmail());
        entity.setTelefon(dto.getTelefon());
        entity.setPib(dto.getPib());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getRejting() != null) {
            entity.setRejting(dto.getRejting());
        }
        entity.setKriterijumi(dto.getKriterijumi());
        return toDto(dobavljacRepository.save(entity));
    }

    public void delete(Long id) {
        if (!dobavljacRepository.existsById(id)) {
            throw new RuntimeException("Dobavljac nije pronadjen sa id: " + id);
        }
        dobavljacRepository.deleteById(id);
    }

    public Dobavljac findEntity(Long id) {
        return dobavljacRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dobavljac nije pronadjen sa id: " + id));
    }

    private DobavljacDto toDto(Dobavljac entity) {
        return DobavljacDto.builder()
                .dobavljacId(entity.getDobavljacId())
                .naziv(entity.getNaziv())
                .grad(entity.getGrad())
                .kontaktEmail(entity.getKontaktEmail())
                .telefon(entity.getTelefon())
                .pib(entity.getPib())
                .status(entity.getStatus())
                .rejting(entity.getRejting())
                .kriterijumi(entity.getKriterijumi())
                .build();
    }
}
