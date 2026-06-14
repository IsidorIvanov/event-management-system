package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.AttachDocumentRequest;
import com.eventsystem.event_management_system.dto.CreateUgovorRequest;
import com.eventsystem.event_management_system.dto.UgovorDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Ugovor;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.UgovorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UgovorService {

    private final UgovorRepository ugovorRepository;
    private final DobavljacService dobavljacService;
    private final DogadjajRepository dogadjajRepository;

    @PreAuthorize("hasAnyRole('MENADZER_DOGADJAJA', 'FINANSIJSKI_KONTROLOR')")
    @Transactional(readOnly = true)
    public List<UgovorDto> getByDobavljac(Long dobavljacId) {
        return ugovorRepository.findByDobavljacDobavljacId(dobavljacId).stream()
                .map(this::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('MENADZER_DOGADJAJA', 'FINANSIJSKI_KONTROLOR')")
    @Transactional(readOnly = true)
    public List<UgovorDto> getActiveByDobavljac(Long dobavljacId) {
        return ugovorRepository.findActiveByDobavljac(dobavljacId, com.eventsystem.event_management_system.utils.enums.StatusUgovora.AKTIVAN, LocalDate.now())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('MENADZER_DOGADJAJA', 'FINANSIJSKI_KONTROLOR')")
    @Transactional(readOnly = true)
    public List<UgovorDto> getAll(Long dobavljacId, Long dogadjajId, com.eventsystem.event_management_system.utils.enums.StatusUgovora status) {
        return ugovorRepository.findAllFiltered(dobavljacId, dogadjajId, status).stream()
                .map(this::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('MENADZER_DOGADJAJA', 'FINANSIJSKI_KONTROLOR')")
    @Transactional(readOnly = true)
    public UgovorDto getById(Long id) {
        return toDto(findEntityWithDetalji(id));
    }

    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    @Transactional
    public UgovorDto create(CreateUgovorRequest request) {
        validateCreateRequest(request);
        String brojUgovora = normalizeRequired(request.getBrojUgovora(), "Broj ugovora");
        if (ugovorRepository.existsByBrojUgovora(brojUgovora)) {
            throw new BadRequestException("Ugovor sa ovim brojem već postoji.");
        }

        Dobavljac dobavljac = dobavljacService.findEntity(request.getDobavljacId());
        Dogadjaj dogadjaj = dogadjajRepository.findById(request.getDogadjajId())
                .orElseThrow(() -> new NotFoundException("Događaj nije pronađen sa id: " + request.getDogadjajId()));

        Ugovor ugovor = Ugovor.builder()
                .brojUgovora(brojUgovora)
                .dobavljac(dobavljac)
                .dogadjaj(dogadjaj)
                .datumPotpisivanja(request.getDatumPotpisivanja())
                .vaziDo(request.getVaziDo())
                .vrednost(request.getVrednost().setScale(2, RoundingMode.HALF_EVEN))
                .predmet(normalizeRequired(request.getPredmet(), "Predmet ugovora"))
                .usloviPlacanja(normalizeNullable(request.getUsloviPlacanja()))
                .status(com.eventsystem.event_management_system.utils.enums.StatusUgovora.NACRT)
                .build();

        return toDto(ugovorRepository.save(ugovor));
    }

    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    @Transactional
    public UgovorDto attachDocument(Long ugovorId, AttachDocumentRequest request) {
        Ugovor ugovor = findEntity(ugovorId);
        if (ugovor.getStatus() != com.eventsystem.event_management_system.utils.enums.StatusUgovora.NACRT
                && ugovor.getStatus() != com.eventsystem.event_management_system.utils.enums.StatusUgovora.AKTIVAN) {
            throw new BadRequestException("Dokument se može dodati samo za ugovor u NACRT ili AKTIVAN statusu.");
        }
        String dokumentUrl = normalizeRequired(request != null ? request.getDokumentUrl() : null, "URL dokumenta");
        if (dokumentUrl.length() > 500) {
            throw new BadRequestException("URL dokumenta može imati najviše 500 karaktera.");
        }
        ugovor.setDokumentUrl(dokumentUrl);
        return toDto(ugovorRepository.save(ugovor));
    }

    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    @Transactional
    public UgovorDto activate(Long ugovorId) {
        Ugovor ugovor = findEntity(ugovorId);
        if (ugovor.getStatus() != com.eventsystem.event_management_system.utils.enums.StatusUgovora.NACRT) {
            throw new BadRequestException("Samo NACRT ugovor može biti aktiviran.");
        }
        if (ugovor.getVaziDo().isBefore(LocalDate.now())) {
            throw new BadRequestException("Nije moguće aktivirati ugovor koji je već istekao.");
        }
        if (ugovor.getDokumentUrl() == null || ugovor.getDokumentUrl().isBlank()) {
            log.warn("Ugovor {} aktiviran bez priloženog dokumenta", ugovorId);
        }
        ugovor.setStatus(com.eventsystem.event_management_system.utils.enums.StatusUgovora.AKTIVAN);
        return toDto(ugovorRepository.save(ugovor));
    }

    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    @Transactional
    public UgovorDto terminate(Long ugovorId) {
        Ugovor ugovor = findEntity(ugovorId);
        if (ugovor.getStatus() != com.eventsystem.event_management_system.utils.enums.StatusUgovora.NACRT
                && ugovor.getStatus() != com.eventsystem.event_management_system.utils.enums.StatusUgovora.AKTIVAN) {
            throw new BadRequestException("Samo NACRT ili AKTIVAN ugovor može biti raskinut.");
        }
        ugovor.setStatus(com.eventsystem.event_management_system.utils.enums.StatusUgovora.RASKINUT);
        return toDto(ugovorRepository.save(ugovor));
    }

    public Ugovor findEntity(Long id) {
        return ugovorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ugovor nije pronađen sa id: " + id));
    }

    private Ugovor findEntityWithDetalji(Long id) {
        return ugovorRepository.findByIdWithDetalji(id)
                .orElseThrow(() -> new NotFoundException("Ugovor nije pronađen sa id: " + id));
    }

    private void validateCreateRequest(CreateUgovorRequest request) {
        if (request == null) {
            throw new BadRequestException("Podaci za ugovor su obavezni.");
        }
        if (request.getDobavljacId() == null) {
            throw new BadRequestException("Dobavljač je obavezan.");
        }
        if (request.getDogadjajId() == null) {
            throw new BadRequestException("Događaj je obavezan.");
        }
        if (request.getDatumPotpisivanja() == null) {
            throw new BadRequestException("Datum potpisivanja je obavezan.");
        }
        if (request.getVaziDo() == null) {
            throw new BadRequestException("Datum isteka je obavezan.");
        }
        if (!request.getVaziDo().isAfter(request.getDatumPotpisivanja())) {
            throw new BadRequestException("Datum isteka mora biti posle datuma potpisivanja.");
        }
        if (request.getVrednost() == null || request.getVrednost().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Vrednost ugovora ne može biti negativna.");
        }
    }

    private UgovorDto toDto(Ugovor entity) {
        return UgovorDto.builder()
                .ugovorId(entity.getUgovorId())
                .brojUgovora(entity.getBrojUgovora())
                .dobavljacId(entity.getDobavljac().getDobavljacId())
                .dobavljacNaziv(entity.getDobavljac().getNaziv())
                .dogadjajId(entity.getDogadjaj().getDogadjajId())
                .dogadjajNaziv(entity.getDogadjaj().getNaziv())
                .nabavkaId(entity.getNabavka() != null ? entity.getNabavka().getNabavkaId() : null)
                .datumPotpisivanja(entity.getDatumPotpisivanja())
                .vrednost(entity.getVrednost())
                .predmet(entity.getPredmet())
                .usloviPlacanja(entity.getUsloviPlacanja())
                .dokumentUrl(entity.getDokumentUrl())
                .status(entity.getStatus())
                .kreiranAt(entity.getKreiranAt())
                .vaziDo(entity.getVaziDo())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " je obavezan.");
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : null;
    }
}
