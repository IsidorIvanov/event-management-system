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
import com.eventsystem.event_management_system.utils.enums.TipTroska;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
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
        fakturaService.recomputePlaceniIznos(f);

        List<Trosak> auto = trosakRepository.findByFakturaIdAndTip(f.getFakturaId(), TipTroska.AUTO_ULAZNA);
        BigDecimal total = auto.stream().map(Trosak::getIznos).filter(x -> x != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<String> recomputeKeys = new java.util.HashSet<>();
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            for (Trosak t : auto) {
                BigDecimal proportion = t.getIznos().divide(total, 6, RoundingMode.HALF_EVEN);
                BigDecimal add = r.getIznos().multiply(proportion).setScale(2, RoundingMode.HALF_EVEN);
                if (t.getRefundiraniIznos() == null) t.setRefundiraniIznos(BigDecimal.ZERO);
                BigDecimal updated = t.getRefundiraniIznos().add(add).setScale(2, RoundingMode.HALF_EVEN);
                if (updated.compareTo(t.getIznos()) > 0) {
                    updated = t.getIznos().setScale(2, RoundingMode.HALF_EVEN);
                }
                t.setRefundiraniIznos(updated);
                if (t.getBudzetId() != null && t.getKategorijaId() != null) {
                    recomputeKeys.add(t.getBudzetId() + ":" + t.getKategorijaId());
                }
            }
            trosakRepository.saveAll(auto);
            for (String key : recomputeKeys) {
                String[] parts = key.split(":");
                budzetService.recomputeStavku(Long.valueOf(parts[0]), Long.valueOf(parts[1]));
            }
        }

        return r;
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
