package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.RefundacijaDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Placanje;
import com.eventsystem.event_management_system.model.Refundacija;
import com.eventsystem.event_management_system.model.Trosak;
import com.eventsystem.event_management_system.repository.PlacanjeRepository;
import com.eventsystem.event_management_system.repository.RefundacijaRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.PlacanjeStatus;
import com.eventsystem.event_management_system.utils.enums.RefundacijaStatus;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
import com.eventsystem.event_management_system.utils.enums.TipTroska;
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
    public Refundacija request(Long placanjeId, RefundacijaDto dto) {
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
        return refundacijaRepository.save(r);
    }

    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    @Transactional
    public Refundacija approve(Long refundacijaId) {
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
        return refundacijaRepository.save(r);
    }

    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    @Transactional
    public Refundacija reject(Long refundacijaId) {
        Refundacija r = findEntity(refundacijaId);
        if (r.getStatus() != RefundacijaStatus.TRAZENA) {
            throw new BadRequestException("Samo TRAZENA refundacija može biti odbijena.");
        }
        r.setStatus(RefundacijaStatus.ODBIJENA);
        r.setOdobrioId(currentUserService.getCurrentZaposleni().getKorisnikId());
        return refundacijaRepository.save(r);
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public Refundacija execute(Long refundacijaId) {
        Refundacija r = findEntity(refundacijaId);
        if (r.getStatus() != RefundacijaStatus.ODOBRENA) {
            throw new BadRequestException("Refundacija mora biti odobrena pre izvršenja.");
        }

        r.setStatus(RefundacijaStatus.IZVRSENA);
        r.setIzvrsenoAt(LocalDateTime.now());
        refundacijaRepository.save(r);

        // Flow 7.2: propagate to Faktura (recompute) and to Trosak.refundiraniIznos proportionally
        var p = r.getPlacanje();
        var f = p.getFaktura();
        if (f.getTip() == TipFakture.ULAZNA) {
            allocateRefundToAutoCosts(f.getFakturaId(), r.getIznos());
        }

        fakturaService.recomputePlaceniIznos(f);
        fakturaService.autoUpdateStatus(f);
        return r;
    }

    private void allocateRefundToAutoCosts(Long fakturaId, BigDecimal refundIznos) {
        List<Trosak> auto = trosakRepository.findByFakturaIdAndTip(fakturaId, TipTroska.AUTO_ULAZNA);
        List<Trosak> eligible = auto.stream()
                .filter(trosak -> remainingRefundableTrosak(trosak).compareTo(BigDecimal.ZERO) > 0)
                .toList();
        BigDecimal totalRemaining = auto.stream()
                .map(this::remainingRefundableTrosak)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);
        if (totalRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal amountToAllocate = refundIznos.setScale(2, RoundingMode.HALF_EVEN).min(totalRemaining);
        BigDecimal allocated = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        Set<String> recomputeKeys = new HashSet<>();

        for (int i = 0; i < eligible.size(); i++) {
            Trosak trosak = eligible.get(i);
            BigDecimal remaining = remainingRefundableTrosak(trosak);

            BigDecimal add;
            if (i == eligible.size() - 1) {
                add = amountToAllocate.subtract(allocated).setScale(2, RoundingMode.HALF_EVEN);
            } else {
                add = amountToAllocate
                        .multiply(remaining)
                        .divide(totalRemaining, 2, RoundingMode.HALF_EVEN);
            }
            if (add.compareTo(remaining) > 0) {
                add = remaining;
            }
            if (add.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            trosak.setRefundiraniIznos(safe(trosak.getRefundiraniIznos()).add(add).setScale(2, RoundingMode.HALF_EVEN));
            allocated = allocated.add(add).setScale(2, RoundingMode.HALF_EVEN);
            if (trosak.getBudzetId() != null && trosak.getKategorijaId() != null) {
                recomputeKeys.add(trosak.getBudzetId() + ":" + trosak.getKategorijaId());
            }
        }

        trosakRepository.saveAll(eligible);
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
}
