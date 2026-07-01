package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.RefundacijaDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.model.Placanje;
import com.eventsystem.event_management_system.model.Refundacija;
import com.eventsystem.event_management_system.model.StavkaFakture;
import com.eventsystem.event_management_system.model.Trosak;
import com.eventsystem.event_management_system.repository.PlacanjeRepository;
import com.eventsystem.event_management_system.repository.RefundacijaRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.PlacanjeStatus;
import com.eventsystem.event_management_system.utils.enums.RefundacijaStatus;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RefundacijaService {

    private final RefundacijaRepository refundacijaRepository;
    private final PlacanjeRepository placanjeRepository;
    private final FakturaService fakturaService;
    private final TrosakRepository trosakRepository;
    private final BudzetService budzetService;
    private final CurrentUserService currentUserService;

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public RefundacijaDto request(Long placanjeId, RefundacijaDto dto) {
        Placanje p = placanjeRepository.findById(placanjeId)
                .orElseThrow(() -> new NotFoundException("Plaćanje nije pronađeno sa id: " + placanjeId));
        if (p.getStatus() != PlacanjeStatus.COMPLETED) {
            throw new BadRequestException("Refundacija je moguća samo za završena plaćanja.");
        }

        BigDecimal iznos = requirePositive(dto != null ? dto.getIznos() : null, "Iznos refundacije");
        BigDecimal vecRezervisano = refundacijaRepository.sumByPlacanjeIdAndStatuses(
                placanjeId,
                EnumSet.of(RefundacijaStatus.TRAZENA, RefundacijaStatus.ODOBRENA, RefundacijaStatus.IZVRSENA)
        );
        BigDecimal preostalo = safe(p.getIznos()).subtract(safe(vecRezervisano));
        if (iznos.compareTo(preostalo) > 0) {
            throw new BadRequestException("Iznos refundacije ne sme preći preostali refundabilni iznos.");
        }
        if (dto == null || dto.getRazlog() == null || dto.getRazlog().trim().isEmpty()) {
            throw new BadRequestException("Razlog refundacije je obavezan.");
        }

        Refundacija r = Refundacija.builder()
                .placanje(p)
                .iznos(iznos)
                .razlog(dto.getRazlog().trim())
                .kreiraoId(currentUserService.getCurrentZaposleni().getKorisnikId())
                .status(RefundacijaStatus.TRAZENA)
                .kreiranoAt(LocalDateTime.now())
                .build();
        return toDto(refundacijaRepository.save(r));
    }

    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    @Transactional
    public RefundacijaDto approve(Long refundacijaId) {
        Refundacija r = findEntity(refundacijaId);
        if (r.getStatus() != RefundacijaStatus.TRAZENA) {
            throw new BadRequestException("Samo TRAZENA refundacija može biti odobrena.");
        }
        Long odobrioId = currentUserService.getCurrentZaposleni().getKorisnikId();
        if (r.getKreiraoId() != null && r.getKreiraoId().equals(odobrioId)) {
            throw new BadRequestException("Odobravalac ne sme biti isti kao tražilac refundacije.");
        }

        r.setStatus(RefundacijaStatus.ODOBRENA);
        r.setOdobrioId(odobrioId);
        return toDto(refundacijaRepository.save(r));
    }

    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    @Transactional
    public RefundacijaDto reject(Long refundacijaId) {
        Refundacija r = findEntity(refundacijaId);
        if (r.getStatus() != RefundacijaStatus.TRAZENA) {
            throw new BadRequestException("Samo TRAZENA refundacija može biti odbijena.");
        }
        r.setStatus(RefundacijaStatus.ODBIJENA);
        r.setOdobrioId(currentUserService.getCurrentZaposleni().getKorisnikId());
        return toDto(refundacijaRepository.save(r));
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public RefundacijaDto execute(Long refundacijaId) {
        Refundacija r = findEntity(refundacijaId);
        if (r.getStatus() != RefundacijaStatus.ODOBRENA) {
            throw new BadRequestException("Refundacija mora biti odobrena pre izvršenja.");
        }

        r.setStatus(RefundacijaStatus.IZVRSENA);
        r.setIzvrsenoAt(LocalDateTime.now());
        refundacijaRepository.save(r);

        var p = r.getPlacanje();
        var f = p.getFaktura();
        fakturaService.recomputePlaceniIznos(f);
        fakturaService.autoUpdateStatus(f);

        // Flow 7.2: for incoming invoices, propagate refund to AUTO_ULAZNA expenses proportionally.
        if (f.getTip() == TipFakture.ULAZNA) {
            allocateRefundToAutoCosts(f, r.getIznos());
        }

        return toDto(r);
    }

    private void allocateRefundToAutoCosts(Faktura faktura, BigDecimal refundIznos) {
        List<StavkaFakture> stavkeSaTroskom = faktura.getStavke().stream()
                .filter(stavka -> stavka.getTrosakId() != null)
                .toList();
        BigDecimal ukupanIznos = safe(faktura.getUkupnaIznos()).setScale(2, RoundingMode.HALF_EVEN);
        if (stavkeSaTroskom.isEmpty() || ukupanIznos.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal amountToAllocate = refundIznos.setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal allocated = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        Set<String> recomputeKeys = new HashSet<>();
        List<Trosak> updated = new java.util.ArrayList<>();

        for (int i = 0; i < stavkeSaTroskom.size(); i++) {
            StavkaFakture stavka = stavkeSaTroskom.get(i);
            Trosak trosak = trosakRepository.findById(stavka.getTrosakId())
                    .orElse(null);
            if (trosak == null) {
                continue;
            }
            BigDecimal remaining = remainingRefundableTrosak(trosak);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal add;
            if (i == stavkeSaTroskom.size() - 1) {
                add = amountToAllocate.subtract(allocated).setScale(2, RoundingMode.HALF_EVEN);
            } else {
                BigDecimal udeo = safe(stavka.getUkupnaCena())
                        .divide(ukupanIznos, 8, RoundingMode.HALF_EVEN);
                add = amountToAllocate
                        .multiply(udeo)
                        .setScale(2, RoundingMode.HALF_EVEN);
            }
            if (add.compareTo(remaining) > 0) {
                add = remaining;
            }
            if (add.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            trosak.setRefundiraniIznos(safe(trosak.getRefundiraniIznos()).add(add).setScale(2, RoundingMode.HALF_EVEN));
            allocated = allocated.add(add).setScale(2, RoundingMode.HALF_EVEN);
            updated.add(trosak);
            if (trosak.getBudzetId() != null && trosak.getKategorijaId() != null) {
                recomputeKeys.add(trosak.getBudzetId() + ":" + trosak.getKategorijaId());
            }
        }

        trosakRepository.saveAll(updated);
        for (String key : recomputeKeys) {
            String[] parts = key.split(":");
            budzetService.recomputeStavku(Long.valueOf(parts[0]), Long.valueOf(parts[1]));
        }
    }

    private BigDecimal remainingRefundableTrosak(Trosak trosak) {
        BigDecimal remaining = safe(trosak.getIznos()).subtract(safe(trosak.getRefundiraniIznos()));
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        return remaining.setScale(2, RoundingMode.HALF_EVEN);
    }

    private Refundacija findEntity(Long refundacijaId) {
        return refundacijaRepository.findById(refundacijaId)
                .orElseThrow(() -> new NotFoundException("Refundacija nije pronađena sa id: " + refundacijaId));
    }

    private BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " je obavezan.");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(fieldName + " mora biti veći od 0.");
        }
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private RefundacijaDto toDto(Refundacija refundacija) {
        return RefundacijaDto.builder()
                .refundacijaId(refundacija.getRefundacijaId())
                .placanjeId(refundacija.getPlacanje() != null ? refundacija.getPlacanje().getPlacanjeId() : null)
                .iznos(refundacija.getIznos())
                .status(refundacija.getStatus())
                .kreiraoId(refundacija.getKreiraoId())
                .odobrioId(refundacija.getOdobrioId())
                .izvrsenoAt(refundacija.getIzvrsenoAt())
                .kreiranoAt(refundacija.getKreiranoAt())
                .razlog(refundacija.getRazlog())
                .build();
    }
}
