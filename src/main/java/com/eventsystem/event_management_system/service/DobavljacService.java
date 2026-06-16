package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DobavljacDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.repository.DobavljacRepository;
import com.eventsystem.event_management_system.repository.NabavkaRepository;
import com.eventsystem.event_management_system.repository.UgovorRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDobavljaca;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DobavljacService {

    private static final Set<StatusNabavke> ZAVRSENE_NABAVKE = Set.of(StatusNabavke.ZAVRSENA, StatusNabavke.ODBIJENA);

    private final DobavljacRepository dobavljacRepository;
    private final UgovorRepository ugovorRepository;
    private final NabavkaRepository nabavkaRepository;

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

    @Transactional
    public DobavljacDto create(DobavljacDto dto) {
        validatePibUnique(dto.getPib(), null);
        Dobavljac entity = Dobavljac.builder()
                .naziv(normalizeRequired(dto.getNaziv(), "Naziv"))
                .grad(normalizeRequired(dto.getGrad(), "Grad"))
                .kontaktEmail(dto.getKontaktEmail())
                .telefon(dto.getTelefon())
                .pib(normalizeRequired(dto.getPib(), "PIB"))
                .status(StatusDobavljaca.AKTIVAN)
                .build();
        return toDto(dobavljacRepository.save(entity));
    }

    @Transactional
    public DobavljacDto update(Long id, DobavljacDto dto) {
        Dobavljac entity = findEntity(id);
        validatePibUnique(dto.getPib(), id);
        entity.setNaziv(normalizeRequired(dto.getNaziv(), "Naziv"));
        entity.setGrad(normalizeRequired(dto.getGrad(), "Grad"));
        entity.setKontaktEmail(dto.getKontaktEmail());
        entity.setTelefon(dto.getTelefon());
        entity.setPib(normalizeRequired(dto.getPib(), "PIB"));
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        entity.setRejting(dto.getRejting());
        entity.setKriterijumi(dto.getKriterijumi());
        return toDto(dobavljacRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        Dobavljac entity = findEntity(id);
        assertCanDeleteOrDeactivate(entity.getDobavljacId());
        dobavljacRepository.delete(entity);
    }

    @Transactional
    public DobavljacDto deactivate(Long id) {
        Dobavljac entity = findEntity(id);
        entity.setStatus(StatusDobavljaca.NEAKTIVAN);
        return toDto(dobavljacRepository.save(entity));
    }

    public Dobavljac findEntity(Long id) {
        return dobavljacRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dobavljac nije pronadjen sa id: " + id));
    }

    public void assertAvailableForNabavka(Long dobavljacId) {
        Dobavljac dobavljac = findEntity(dobavljacId);
        if (dobavljac.getStatus() == StatusDobavljaca.SUSPENDOVAN) {
            throw new BadRequestException("Dobavljač je suspendovan i ne može biti izabran za nabavku.");
        }
        if (dobavljac.getStatus() == StatusDobavljaca.NEAKTIVAN) {
            throw new BadRequestException("Dobavljač je neaktivan i ne može biti izabran za nabavku.");
        }
    }

    private void validatePibUnique(String pib, Long excludeId) {
        String normalized = normalizeRequired(pib, "PIB");
        boolean exists = excludeId == null
                ? dobavljacRepository.existsByPib(normalized)
                : dobavljacRepository.existsByPibAndDobavljacIdNot(normalized, excludeId);
        if (exists) {
            throw new BadRequestException("Dobavljač sa ovim PIB-om već postoji.");
        }
    }

    private void assertCanDeleteOrDeactivate(Long dobavljacId) {
        if (ugovorRepository.existsByDobavljacDobavljacIdAndStatus(dobavljacId, StatusUgovora.AKTIVAN)) {
            throw new BadRequestException("Dobavljač ima aktivan ugovor i ne može biti obrisan.");
        }
        if (nabavkaRepository.existsByDobavljacDobavljacIdAndStatusNotIn(dobavljacId, ZAVRSENE_NABAVKE)) {
            throw new BadRequestException("Dobavljač ima otvorene nabavke i ne može biti obrisan.");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " je obavezan.");
        }
        return value.trim();
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
