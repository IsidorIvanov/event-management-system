package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.PlacanjeDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.model.Placanje;
import com.eventsystem.event_management_system.repository.PlacanjeRepository;
import com.eventsystem.event_management_system.repository.UgovorRepository;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.PlacanjeStatus;
import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlacanjeService {

    private static final Set<FakturaStatus> PAYABLE_STATUSES = EnumSet.of(
            FakturaStatus.IZDATA,
            FakturaStatus.DELIMICNO_PLACENA,
            FakturaStatus.DOSPELA
    );

    private final PlacanjeRepository placanjeRepository;
    private final FakturaService fakturaService;
    private final UgovorRepository ugovorRepository;

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public Placanje create(Long fakturaId, PlacanjeDto dto) {
        Faktura f = fakturaService.findEntity(fakturaId);
        validateCreate(f, dto);

        Placanje p = Placanje.builder()
                .faktura(f)
                .iznos(dto.getIznos().setScale(2, RoundingMode.HALF_EVEN))
                .metod(dto.getMetod())
                .napomena(dto.getNapomena())
                .status(PlacanjeStatus.PENDING)
                .kreiranoAt(LocalDateTime.now())
                .build();
        p = placanjeRepository.save(p);
        f.getPlacanja().add(p);
        return p;
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public Placanje confirm(Long placanjeId) {
        Placanje p = findEntity(placanjeId);
        if (p.getStatus() != PlacanjeStatus.PENDING) {
            throw new BadRequestException("Samo PENDING plaćanje može biti potvrđeno.");
        }
        p.setStatus(PlacanjeStatus.COMPLETED);
        p.setPotvrdjenoAt(LocalDateTime.now());
        placanjeRepository.save(p);

        // algoritam 8.2
        fakturaService.recomputePlaceniIznos(p.getFaktura());
        return p;
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public Placanje fail(Long placanjeId) {
        Placanje p = findEntity(placanjeId);
        if (p.getStatus() != PlacanjeStatus.PENDING) {
            throw new BadRequestException("Samo PENDING plaćanje može biti označeno kao neuspešno.");
        }
        p.setStatus(PlacanjeStatus.FAILED);
        return placanjeRepository.save(p);
    }

    private Placanje findEntity(Long placanjeId) {
        return placanjeRepository.findById(placanjeId)
                .orElseThrow(() -> new NotFoundException("Plaćanje nije pronađeno sa id: " + placanjeId));
    }

    private void validateCreate(Faktura faktura, PlacanjeDto dto) {
        if (dto == null) {
            throw new BadRequestException("Podaci za plaćanje su obavezni.");
        }
        if (!PAYABLE_STATUSES.contains(faktura.getStatus())) {
            throw new BadRequestException("Plaćanje je moguće samo za izdate, dospele ili delimično plaćene fakture.");
        }
        if (dto.getIznos() == null || dto.getIznos().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Iznos plaćanja mora biti veći od 0.");
        }
        if (dto.getMetod() == null) {
            throw new BadRequestException("Metod plaćanja je obavezan.");
        }

        BigDecimal preostalo = safe(faktura.getUkupnaIznos()).subtract(safe(faktura.getPlaceniIznos()));
        if (dto.getIznos().compareTo(preostalo) > 0) {
            throw new BadRequestException("Iznos plaćanja ne sme preći preostali dug fakture.");
        }

        if (faktura.getTip() == TipFakture.ULAZNA) {
            if (faktura.getDobavljacId() == null) {
                throw new BadRequestException("Ulazna faktura mora imati dobavljača pre plaćanja.");
            }
            boolean hasActiveContract = ugovorRepository.existsActiveForDobavljac(
                    faktura.getDobavljacId(),
                    StatusUgovora.AKTIVAN,
                    LocalDate.now()
            );
            if (!hasActiveContract) {
                throw new BadRequestException("Plaćanje ulazne fakture je moguće samo uz aktivan ugovor dobavljača.");
            }
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
