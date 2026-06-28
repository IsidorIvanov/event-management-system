package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.FinansijskiIzvestajDto;
import com.eventsystem.event_management_system.dto.GenerateIzvestajRequest;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.ConflictException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.FinansijskiIzvestaj;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.repository.FinansijskiIzvestajRepository;
import com.eventsystem.event_management_system.utils.enums.FormatIzvestaja;
import com.eventsystem.event_management_system.utils.enums.IzvestajStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinansijskiIzvestajService {

    private final FinansijskiIzvestajRepository finansijskiIzvestajRepository;
    private final FinansijskiIzvestajStatusTx statusTx;
    private final FinansijskiIzvestajGenerateTx generateTx;
    private final LocalFileStorageService fileStorageService;
    private final CurrentUserService currentUserService;

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    public FinansijskiIzvestajDto generate(GenerateIzvestajRequest request) {
        validateRequest(request);
        Zaposleni kreirao = currentUserService.getCurrentZaposleni();
        Long izvestajId = statusTx.createPending(request, kreirao);
        try {
            FinansijskiIzvestaj saved = generateTx.run(izvestajId);
            return toDto(saved);
        } catch (Exception ex) {
            statusTx.markFailed(izvestajId, ex.getMessage());
            return getById(izvestajId);
        }
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public List<FinansijskiIzvestajDto> list() {
        Zaposleni current = currentUserService.getCurrentZaposleni();
        return finansijskiIzvestajRepository.findByKreiraoOrderByKreiranAtDesc(current).stream()
                .map(this::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public FinansijskiIzvestajDto getById(Long id) {
        return toDto(findEntity(id));
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(Long id) {
        FinansijskiIzvestaj izvestaj = findEntity(id);
        if (izvestaj.getStatus() != IzvestajStatus.READY) {
            throw new ConflictException("Preuzimanje je moguće samo za izveštaje u statusu READY.");
        }
        if (izvestaj.getFileUrl() == null || izvestaj.getFileUrl().isBlank()) {
            throw new NotFoundException("Fajl izveštaja nije pronađen.");
        }

        Resource resource = fileStorageService.loadAsResource(izvestaj.getFileUrl());
        String filename = "izvestaj-" + id + (izvestaj.getFormat() == FormatIzvestaja.PDF ? ".pdf" : ".xlsx");
        String contentType = izvestaj.getFormat() == FormatIzvestaja.PDF
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    void validateRequest(GenerateIzvestajRequest request) {
        if (request == null || request.getTip() == null || request.getFormat() == null) {
            throw new BadRequestException("Tip i format izveštaja su obavezni.");
        }

        switch (request.getTip()) {
            case PO_DOGADJAJU -> {
                if (request.getDogadjajId() == null) {
                    throw new BadRequestException("ID događaja je obavezan za izveštaj po događaju.");
                }
                if (request.getKlijentId() != null || request.getDobavljacId() != null) {
                    throw new BadRequestException("Izveštaj po događaju ne sme imati klijentId ili dobavljacId.");
                }
                if (request.getPeriodOd() != null && request.getPeriodDo() != null) {
                    validatePeriod(request.getPeriodOd(), request.getPeriodDo());
                } else if (request.getPeriodOd() != null || request.getPeriodDo() != null) {
                    throw new BadRequestException("Period mora imati i datum od i datum do.");
                }
            }
            case PO_KLIJENTU -> {
                if (request.getKlijentId() == null) {
                    throw new BadRequestException("ID klijenta je obavezan.");
                }
                requirePeriod(request);
                if (request.getDogadjajId() != null || request.getDobavljacId() != null) {
                    throw new BadRequestException("Izveštaj po klijentu ne sme imati dogadjajId ili dobavljacId.");
                }
            }
            case PO_DOBAVLJACU -> {
                if (request.getDobavljacId() == null) {
                    throw new BadRequestException("ID dobavljača je obavezan.");
                }
                requirePeriod(request);
                if (request.getDogadjajId() != null || request.getKlijentId() != null) {
                    throw new BadRequestException("Izveštaj po dobavljaču ne sme imati dogadjajId ili klijentId.");
                }
            }
            case PO_PERIODU -> {
                requirePeriod(request);
                if (request.getDogadjajId() != null || request.getKlijentId() != null || request.getDobavljacId() != null) {
                    throw new BadRequestException("Izveštaj po periodu ne sme imati dogadjajId, klijentId ili dobavljacId.");
                }
            }
        }
    }

    private void requirePeriod(GenerateIzvestajRequest request) {
        if (request.getPeriodOd() == null || request.getPeriodDo() == null) {
            throw new BadRequestException("Period (od/do) je obavezan.");
        }
        validatePeriod(request.getPeriodOd(), request.getPeriodDo());
    }

    private void validatePeriod(LocalDate od, LocalDate doDate) {
        if (doDate.isBefore(od)) {
            throw new BadRequestException("Datum kraja perioda mora biti posle ili jednak datumu početka.");
        }
    }

    private FinansijskiIzvestaj findEntity(Long id) {
        if (id == null) {
            throw new BadRequestException("ID izveštaja je obavezan.");
        }
        return finansijskiIzvestajRepository.findByIdWithKreirao(id)
                .orElseThrow(() -> new NotFoundException("Finansijski izveštaj nije pronađen sa id: " + id));
    }

    private FinansijskiIzvestajDto toDto(FinansijskiIzvestaj entity) {
        Zaposleni kreirao = entity.getKreirao();
        return FinansijskiIzvestajDto.builder()
                .izvestajId(entity.getIzvestajId())
                .tip(entity.getTip())
                .format(entity.getFormat())
                .status(entity.getStatus())
                .dogadjajId(entity.getDogadjajId())
                .klijentId(entity.getKlijentId())
                .dobavljacId(entity.getDobavljacId())
                .periodOd(entity.getPeriodOd())
                .periodDo(entity.getPeriodDo())
                .fileUrl(entity.getFileUrl())
                .greskaPoruka(entity.getGreskaPoruka())
                .kreiraoId(kreirao != null ? kreirao.getKorisnikId() : null)
                .kreiraoImePrezime(kreirao != null ? (kreirao.getIme() + " " + kreirao.getPrezime()).trim() : null)
                .kreiranAt(entity.getKreiranAt())
                .generisanoAt(entity.getGenerisanoAt())
                .build();
    }
}
