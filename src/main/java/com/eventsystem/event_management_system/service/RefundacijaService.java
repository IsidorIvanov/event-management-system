package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.RefundacijaDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundacijaService {

    private final RefundacijaRepository refundacijaRepository;
    private final PlacanjeRepository placanjeRepository;
    private final FakturaService fakturaService;
    private final TrosakRepository trosakRepository;

    @Transactional
    public Refundacija request(Long placanjeId, RefundacijaDto dto, Long kreiraoId) {
        Placanje p = placanjeRepository.findById(placanjeId)
            .orElseThrow(() -> new RuntimeException("Placanje not found"));
        if (p.getStatus() != PlacanjeStatus.COMPLETED) {
            throw new RuntimeException("Refundacija je moguća samo za završena plaćanja");
        }
        Refundacija r = Refundacija.builder()
            .placanje(p)
                .iznos(dto.getIznos())
                .razlog(dto.getRazlog())
                .kreiraoId(kreiraoId)
                .status(RefundacijaStatus.TRAZENA)
                .kreiranoAt(LocalDateTime.now())
                .build();
        return refundacijaRepository.save(r);
    }

    @Transactional
    public Refundacija approve(Long refundacijaId, Long odobrioId) {
        Refundacija r = refundacijaRepository.findById(refundacijaId).orElseThrow(() -> new RuntimeException("Refundacija not found"));
        if (r.getKreiraoId() != null && r.getKreiraoId().equals(odobrioId)) {
            throw new RuntimeException("Separation of duty: odobravalac ne sme biti isti kao tražilac refundacije");
        }

        r.setStatus(RefundacijaStatus.ODOBRENA);
        r.setOdobrioId(odobrioId);
        return refundacijaRepository.save(r);
    }

    @Transactional
    public Refundacija reject(Long refundacijaId, Long odobrioId) {
        Refundacija r = refundacijaRepository.findById(refundacijaId).orElseThrow(() -> new RuntimeException("Refundacija not found"));
        r.setStatus(RefundacijaStatus.ODBIJENA);
        r.setOdobrioId(odobrioId);
        return refundacijaRepository.save(r);
    }

    @Transactional
    public Refundacija execute(Long refundacijaId) {
        Refundacija r = refundacijaRepository.findById(refundacijaId).orElseThrow(() -> new RuntimeException("Refundacija not found"));
        if (r.getStatus() != RefundacijaStatus.ODOBRENA) {
            throw new RuntimeException("Refundacija must be odobrena before execution");
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
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            for (Trosak t : auto) {
                BigDecimal proportion = t.getIznos().divide(total, 6, RoundingMode.HALF_UP);
                BigDecimal add = r.getIznos().multiply(proportion);
                if (t.getRefundiraniIznos() == null) t.setRefundiraniIznos(BigDecimal.ZERO);
                t.setRefundiraniIznos(t.getRefundiraniIznos().add(add));
            }
            trosakRepository.saveAll(auto);
        }

        return r;
    }
}
