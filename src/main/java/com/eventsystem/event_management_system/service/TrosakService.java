package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.TrosakDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Budzet;
import com.eventsystem.event_management_system.model.StavkaBudzeta;
import com.eventsystem.event_management_system.model.Trosak;
import com.eventsystem.event_management_system.model.compositePK.StavkaBudzetaId;
import com.eventsystem.event_management_system.repository.StavkaBudzetaRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.BudzetStatus;
import com.eventsystem.event_management_system.utils.enums.TipTroska;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrosakService {

    private final TrosakRepository trosakRepository;
    private final StavkaBudzetaRepository stavkaBudzetaRepository;
    private final BudzetService budzetService;
    private final CurrentUserService currentUserService;

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'KOORDINATOR_RESURSA', 'KOORDINATOR_PROGRAMA')")
    @Transactional
    public TrosakDto createManual(TrosakDto dto) {
        validateDto(dto);
        StavkaBudzeta stavka = findStavka(dto.getBudzetId(), dto.getKategorijaId());
        Budzet budzet = stavka.getBudzet();

        if (budzet.getStatus() != BudzetStatus.ACTIVE) {
            throw new BadRequestException("Trošak se može evidentirati samo za ACTIVE budžet.");
        }
        if (!dto.getDogadjajId().equals(budzet.getDogadjaj().getDogadjajId())) {
            throw new BadRequestException("Događaj troška mora biti isti kao događaj budžeta.");
        }

        Long evidentiraoId = dto.getEvidentiraoId() != null
                ? dto.getEvidentiraoId()
                : currentUserService.getCurrentZaposleni().getKorisnikId();

        Trosak trosak = Trosak.builder()
                .budzetId(dto.getBudzetId())
                .kategorijaId(dto.getKategorijaId())
                .dogadjajId(dto.getDogadjajId())
                .dobavljacId(dto.getDobavljacId())
                .evidentiraoId(evidentiraoId)
                .opis(dto.getOpis().trim())
                .iznos(requireNonNegative(dto.getIznos(), "Iznos troška"))
                .refundiraniIznos(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN))
                .datumTroska(dto.getDatumTroska())
                .tip(dto.getTip())
                .kreiranAt(LocalDateTime.now())
                .build();

        Trosak saved = trosakRepository.save(trosak);
        budzetService.recomputeStavku(dto.getBudzetId(), dto.getKategorijaId());
        return toDto(saved);
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public List<TrosakDto> getByStavka(Long budzetId, Long kategorijaId) {
        return trosakRepository.findByBudzetIdAndKategorijaIdOrderByKreiranAtDesc(budzetId, kategorijaId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA', 'KOORDINATOR_RESURSA', 'KOORDINATOR_PROGRAMA')")
    @Transactional(readOnly = true)
    public List<TrosakDto> getAll(Long dogadjajId, Long budzetId, Long kategorijaId, TipTroska tip) {
        return trosakRepository.findAllFiltered(dogadjajId, budzetId, kategorijaId, tip)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void validateDto(TrosakDto dto) {
        if (dto == null) {
            throw new BadRequestException("Podaci za trošak su obavezni.");
        }
        if (dto.getTip() == null) {
            throw new BadRequestException("Tip troška je obavezan.");
        }
        if (dto.getTip() != TipTroska.RUCNI && dto.getTip() != TipTroska.GOTOVINSKI) {
            throw new BadRequestException(
                    "Dozvoljeni tipovi troška su RUCNI i GOTOVINSKI. "
                            + "AUTO_ULAZNA trošak se kreira isključivo kroz ulaznu fakturu."
            );
        }
        if (dto.getOpis() == null || dto.getOpis().trim().isEmpty()) {
            throw new BadRequestException("Opis troška je obavezan.");
        }
        if (dto.getDatumTroska() == null) {
            throw new BadRequestException("Datum troška je obavezan.");
        }
        requireNonNegative(dto.getIznos(), "Iznos troška");
    }

    private StavkaBudzeta findStavka(Long budzetId, Long kategorijaId) {
        if (budzetId == null || kategorijaId == null) {
            throw new BadRequestException("Budžet i kategorija su obavezni.");
        }
        return stavkaBudzetaRepository.findById(new StavkaBudzetaId(budzetId, kategorijaId))
                .orElseThrow(() -> new NotFoundException("Stavka budžeta nije pronađena za izabranu kategoriju."));
    }

    private BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " je obavezan.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(fieldName + " ne može biti negativan.");
        }
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    private TrosakDto toDto(Trosak trosak) {
        return TrosakDto.builder()
                .trosakId(trosak.getTrosakId())
                .budzetId(trosak.getBudzetId())
                .kategorijaId(trosak.getKategorijaId())
                .dogadjajId(trosak.getDogadjajId())
                .dobavljacId(trosak.getDobavljacId())
                .evidentiraoId(trosak.getEvidentiraoId())
                .fakturaId(trosak.getFakturaId())
                .stavkaFaktureId(trosak.getStavkaFaktureId())
                .opis(trosak.getOpis())
                .iznos(trosak.getIznos())
                .refundiraniIznos(trosak.getRefundiraniIznos())
                .netoIznos(safe(trosak.getIznos()).subtract(safe(trosak.getRefundiraniIznos())).setScale(2, RoundingMode.HALF_EVEN))
                .datumTroska(trosak.getDatumTroska())
                .tip(trosak.getTip())
                .kreiranAt(trosak.getKreiranAt())
                .build();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
