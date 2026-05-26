package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.BudzetDto;
import com.eventsystem.event_management_system.dto.BudzetAlertDto;
import com.eventsystem.event_management_system.dto.StavkaBudzetaDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Budzet;
import com.eventsystem.event_management_system.model.BudzetKategorija;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.StavkaBudzeta;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.model.compositePK.StavkaBudzetaId;
import com.eventsystem.event_management_system.repository.BudzetRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.StavkaBudzetaRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.BudzetStatus;
import com.eventsystem.event_management_system.utils.enums.AlertLevel;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import com.eventsystem.event_management_system.utils.enums.StatusKontrole;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudzetService {

    private static final BigDecimal DEFAULT_PRAG_UPOZORENJA = new BigDecimal("0.8000");
    private static final BigDecimal DEFAULT_PRAG_KRITICNOG = new BigDecimal("0.9500");
    private static final BigDecimal TOL_SK = new BigDecimal("0.02");
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final BudzetRepository budzetRepository;
    private final StavkaBudzetaRepository stavkaBudzetaRepository;
    private final DogadjajRepository dogadjajRepository;
    private final BudzetKategorijaService kategorijaService;
    private final CurrentUserService currentUserService;
    private final TrosakRepository trosakRepository;
    private final EntityManager entityManager;
    private final BudzetAlertService budzetAlertService;

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA', 'KOORDINATOR_RESURSA', 'KOORDINATOR_PROGRAMA')")
    @Transactional(readOnly = true)
    public List<BudzetDto> getAll() {
        return budzetRepository.findAllWithDetalji().stream().map(this::toDto).toList();
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA', 'KOORDINATOR_RESURSA', 'KOORDINATOR_PROGRAMA')")
    @Transactional(readOnly = true)
    public BudzetDto getById(Long id) {
        return toDto(findEntityWithDetalji(id));
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA', 'KOORDINATOR_RESURSA', 'KOORDINATOR_PROGRAMA')")
    @Transactional(readOnly = true)
    public List<BudzetDto> getByDogadjaj(Long dogadjajId) {
        return budzetRepository.findByDogadjajIdWithDetalji(dogadjajId).stream().map(this::toDto).toList();
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public BudzetDto create(BudzetDto dto) {
        BigDecimal planiraniIznos = requireNonNegative(dto.getPlaniraniIznos(), "Planirani iznos");
        Dogadjaj dogadjaj = dogadjajRepository.findById(dto.getDogadjajId())
                .orElseThrow(() -> new NotFoundException("Događaj nije pronađen sa id: " + dto.getDogadjajId()));
        Zaposleni kreirao = currentUserService.getCurrentZaposleni();

        Budzet budzet = Budzet.builder()
                .dogadjaj(dogadjaj)
                .kreirao(kreirao)
                .nazivBudzeta(normalizeNaziv(dto.getNazivBudzeta()))
                .planiraniIznos(planiraniIznos)
                .odobreniIznos(planiraniIznos)
                .status(BudzetStatus.DRAFT)
                .build();

        return toDto(budzetRepository.save(budzet));
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public BudzetDto update(Long id, BudzetDto dto) {
        Budzet budzet = findEntityWithDetalji(id);
        requireStatus(budzet, BudzetStatus.DRAFT, "Budžet se može menjati samo dok je u DRAFT statusu.");

        BigDecimal planiraniIznos = requireNonNegative(dto.getPlaniraniIznos(), "Planirani iznos");
        BigDecimal odobreniIznos = dto.getOdobreniIznos() != null
                ? requireNonNegative(dto.getOdobreniIznos(), "Odobreni iznos")
                : planiraniIznos;

        BigDecimal sumaStavki = safe(stavkaBudzetaRepository.sumPlaniraniByBudzetId(id));
        if (sumaStavki.compareTo(odobreniIznos) > 0) {
            throw new BadRequestException("Odobreni iznos ne može biti manji od zbira planiranih stavki.");
        }

        budzet.setNazivBudzeta(normalizeNaziv(dto.getNazivBudzeta()));
        budzet.setPlaniraniIznos(planiraniIznos);
        budzet.setOdobreniIznos(odobreniIznos);
        return toDto(budzetRepository.save(budzet));
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public void delete(Long id) {
        Budzet budzet = findEntityWithDetalji(id);
        requireStatus(budzet, BudzetStatus.DRAFT, "Budžet se može obrisati samo dok je u DRAFT statusu.");
        budzetRepository.delete(budzet);
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public BudzetDto addStavka(Long budzetId, StavkaBudzetaDto dto) {
        Budzet budzet = findEntityWithDetalji(budzetId);
        requireStatus(budzet, BudzetStatus.DRAFT, "Stavke se mogu dodavati samo dok je budžet u DRAFT statusu.");

        BudzetKategorija kategorija = kategorijaService.findEntity(dto.getKategorijaId());
        StavkaBudzetaId id = new StavkaBudzetaId(budzetId, dto.getKategorijaId());
        if (stavkaBudzetaRepository.existsById(id)) {
            throw new BadRequestException("Stavka za izabranu kategoriju već postoji u ovom budžetu.");
        }

        BigDecimal planiraniIznos = requireNonNegative(dto.getPlaniraniIznos(), "Planirani iznos stavke");
        validatePlanLimit(budzet, safe(stavkaBudzetaRepository.sumPlaniraniByBudzetId(budzetId)).add(planiraniIznos));

        StavkaBudzeta stavka = StavkaBudzeta.builder()
                .id(id)
                .budzet(budzet)
                .kategorija(kategorija)
                .planiraniIznos(planiraniIznos)
                .stvarniIznos(BigDecimal.ZERO)
                .pragUpozorenja(resolvePrag(dto.getPragUpozorenja(), DEFAULT_PRAG_UPOZORENJA, "Prag upozorenja"))
                .pragKriticnog(resolvePrag(dto.getPragKriticnog(), DEFAULT_PRAG_KRITICNOG, "Kritični prag"))
                .komentar(dto.getKomentar())
                .build();
        validatePragovi(stavka.getPragUpozorenja(), stavka.getPragKriticnog());
        stavka.setStatusKontrole(calculateStatusKontrole(stavka.getPlaniraniIznos(), stavka.getStvarniIznos()));

        stavkaBudzetaRepository.save(stavka);
        return getById(budzetId);
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public BudzetDto updateStavka(Long budzetId, Long kategorijaId, StavkaBudzetaDto dto) {
        Budzet budzet = findEntityWithDetalji(budzetId);
        if (budzet.getStatus() != BudzetStatus.DRAFT && budzet.getStatus() != BudzetStatus.ACTIVE) {
            throw new BadRequestException("Stavke se mogu menjati samo dok je budžet u DRAFT ili ACTIVE statusu.");
        }

        StavkaBudzeta stavka = findStavka(budzetId, kategorijaId);
        BigDecimal noviPlan = requireNonNegative(dto.getPlaniraniIznos(), "Planirani iznos stavke");
        BigDecimal trenutnaSuma = safe(stavkaBudzetaRepository.sumPlaniraniByBudzetId(budzetId));
        BigDecimal novaSuma = trenutnaSuma.subtract(safe(stavka.getPlaniraniIznos())).add(noviPlan);
        validatePlanLimit(budzet, novaSuma);

        BigDecimal pragUpozorenja = resolvePrag(dto.getPragUpozorenja(), stavka.getPragUpozorenja(), "Prag upozorenja");
        BigDecimal pragKriticnog = resolvePrag(dto.getPragKriticnog(), stavka.getPragKriticnog(), "Kritični prag");
        validatePragovi(pragUpozorenja, pragKriticnog);

        stavka.setPlaniraniIznos(noviPlan);
        stavka.setPragUpozorenja(pragUpozorenja);
        stavka.setPragKriticnog(pragKriticnog);
        stavka.setKomentar(dto.getKomentar());
        stavka.setStatusKontrole(calculateStatusKontrole(noviPlan, safe(stavka.getStvarniIznos())));

        stavkaBudzetaRepository.save(stavka);
        budzetAlertService.evaluateAndRemember(
                budzetId,
                kategorijaId,
                stavka.getKategorija().getNaziv(),
                stavka.getStvarniIznos(),
                stavka.getPlaniraniIznos(),
                stavka.getPragUpozorenja(),
                stavka.getPragKriticnog()
        );
        return getById(budzetId);
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public BudzetDto deleteStavka(Long budzetId, Long kategorijaId) {
        Budzet budzet = findEntityWithDetalji(budzetId);
        requireStatus(budzet, BudzetStatus.DRAFT, "Stavke se mogu brisati samo dok je budžet u DRAFT statusu.");
        StavkaBudzeta stavka = findStavka(budzetId, kategorijaId);
        budzet.getStavke().removeIf(s -> s.getId().equals(stavka.getId()));
        stavkaBudzetaRepository.delete(stavka);
        stavkaBudzetaRepository.flush();
        entityManager.clear();
        return getById(budzetId);
    }

    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    @Transactional
    public BudzetDto approve(Long id) {
        Budzet budzet = findEntityWithDetalji(id);
        requireStatus(budzet, BudzetStatus.DRAFT, "Samo DRAFT budžet može biti odobren.");

        if (stavkaBudzetaRepository.countByBudzetBudzetId(id) < 1) {
            throw new BadRequestException("Budžet mora imati najmanje jednu stavku pre odobravanja.");
        }
        BigDecimal sumaStavki = safe(stavkaBudzetaRepository.sumPlaniraniByBudzetId(id));
        validatePlanLimit(budzet, sumaStavki);

        budzet.setStatus(BudzetStatus.APPROVED);
        return toDto(budzetRepository.save(budzet));
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public BudzetDto activate(Long id) {
        Budzet budzet = findEntityWithDetalji(id);
        requireStatus(budzet, BudzetStatus.APPROVED, "Samo APPROVED budžet može biti aktiviran.");

        StatusDogadjaja statusDogadjaja = budzet.getDogadjaj().getStatus();
        if (statusDogadjaja != StatusDogadjaja.OBJAVLJEN && statusDogadjaja != StatusDogadjaja.AKTIVAN) {
            throw new BadRequestException("Budžet se može aktivirati samo za OBJAVLJEN ili AKTIVAN događaj.");
        }

        budzet.setStatus(BudzetStatus.ACTIVE);
        return toDto(budzetRepository.save(budzet));
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public BudzetDto close(Long id) {
        Budzet budzet = findEntityWithDetalji(id);
        requireStatus(budzet, BudzetStatus.ACTIVE, "Samo ACTIVE budžet može biti zatvoren.");

        if (budzet.getDogadjaj().getStatus() != StatusDogadjaja.ZAVRSEN) {
            throw new BadRequestException("Budžet se može zatvoriti samo kada je događaj ZAVRSEN.");
        }

        budzet.setStatus(BudzetStatus.CLOSED);
        budzet.setZatvorenAt(LocalDateTime.now());
        return toDto(budzetRepository.save(budzet));
    }

    public Budzet findEntityWithDetalji(Long id) {
        return budzetRepository.findByIdWithDetalji(id)
                .orElseThrow(() -> new NotFoundException("Budžet nije pronađen sa id: " + id));
    }

    @Transactional
    public void recomputeStavku(Long budzetId, Long kategorijaId) {
        StavkaBudzeta stavka = findStavka(budzetId, kategorijaId);
        BigDecimal stvarniIznos = safe(trosakRepository.sumNetoByBudzetAndKategorija(budzetId, kategorijaId))
                .setScale(2, RoundingMode.HALF_EVEN);
        stavka.setStvarniIznos(stvarniIznos);
        stavka.setStatusKontrole(calculateStatusKontrole(stavka.getPlaniraniIznos(), stvarniIznos));
        stavkaBudzetaRepository.save(stavka);
        budzetAlertService.evaluateAndRemember(
                budzetId,
                kategorijaId,
                stavka.getKategorija().getNaziv(),
                stvarniIznos,
                stavka.getPlaniraniIznos(),
                stavka.getPragUpozorenja(),
                stavka.getPragKriticnog()
        );
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public List<BudzetAlertDto> getAlerts(Long budzetId) {
        Budzet budzet = findEntityWithDetalji(budzetId);
        return budzet.getStavke().stream()
                .map(this::toAlertDto)
                .filter(alert -> alert.getAlertLevel() != AlertLevel.NONE)
                .toList();
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public List<BudzetAlertDto> getActiveAlerts() {
        return budzetRepository.findAllWithDetalji().stream()
                .flatMap(budzet -> budzet.getStavke().stream())
                .map(this::toAlertDto)
                .filter(alert -> alert.getAlertLevel() != AlertLevel.NONE)
                .toList();
    }

    private StavkaBudzeta findStavka(Long budzetId, Long kategorijaId) {
        return stavkaBudzetaRepository.findById(new StavkaBudzetaId(budzetId, kategorijaId))
                .orElseThrow(() -> new NotFoundException("Stavka budžeta nije pronađena za izabranu kategoriju."));
    }

    private void validatePlanLimit(Budzet budzet, BigDecimal planiraniZbir) {
        if (planiraniZbir.compareTo(safe(budzet.getOdobreniIznos())) > 0) {
            throw new BadRequestException("Zbir planiranih stavki ne sme preći odobreni iznos budžeta.");
        }
    }

    private void validatePragovi(BigDecimal pragUpozorenja, BigDecimal pragKriticnog) {
        if (pragKriticnog.compareTo(pragUpozorenja) <= 0) {
            throw new BadRequestException("Kritični prag mora biti veći od praga upozorenja.");
        }
    }

    private void requireStatus(Budzet budzet, BudzetStatus expected, String message) {
        if (budzet.getStatus() != expected) {
            throw new BadRequestException(message);
        }
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

    private BigDecimal resolvePrag(BigDecimal value, BigDecimal fallback, String fieldName) {
        BigDecimal resolved = value != null ? value : fallback;
        if (resolved.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(fieldName + " ne može biti negativan.");
        }
        return resolved.setScale(4, RoundingMode.HALF_EVEN);
    }

    private StatusKontrole calculateStatusKontrole(BigDecimal planiraniIznos, BigDecimal stvarniIznos) {
        BigDecimal planirani = safe(planiraniIznos);
        BigDecimal stvarni = safe(stvarniIznos);

        if (planirani.compareTo(BigDecimal.ZERO) == 0) {
            return stvarni.compareTo(BigDecimal.ZERO) == 0 ? StatusKontrole.NA_PLANU : StatusKontrole.PREKORACENJE;
        }

        BigDecimal ratio = stvarni.divide(planirani, 6, RoundingMode.HALF_EVEN);
        if (ratio.compareTo(ONE.subtract(TOL_SK)) < 0) {
            return StatusKontrole.ISPOD_PLANA;
        }
        if (ratio.compareTo(ONE.add(TOL_SK)) <= 0) {
            return StatusKontrole.NA_PLANU;
        }
        return StatusKontrole.PREKORACENJE;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String normalizeNaziv(String naziv) {
        if (naziv == null || naziv.trim().isEmpty()) {
            throw new BadRequestException("Naziv budžeta je obavezan.");
        }
        return naziv.trim();
    }

    private BudzetDto toDto(Budzet budzet) {
        List<StavkaBudzetaDto> stavke = budzet.getStavke().stream()
                .sorted(Comparator.comparing(s -> s.getKategorija().getNaziv(), String.CASE_INSENSITIVE_ORDER))
                .map(this::toStavkaDto)
                .toList();
        BigDecimal ukupnoPlanirano = stavke.stream()
                .map(StavkaBudzetaDto::getPlaniraniIznos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ukupnoStvarno = stavke.stream()
                .map(StavkaBudzetaDto::getStvarniIznos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BudzetDto.builder()
                .budzetId(budzet.getBudzetId())
                .dogadjajId(budzet.getDogadjaj().getDogadjajId())
                .dogadjajNaziv(budzet.getDogadjaj().getNaziv())
                .dogadjajStatus(budzet.getDogadjaj().getStatus().name())
                .kreiraoId(budzet.getKreirao().getKorisnikId())
                .kreiraoIme(budzet.getKreirao().getIme() + " " + budzet.getKreirao().getPrezime())
                .nazivBudzeta(budzet.getNazivBudzeta())
                .planiraniIznos(safe(budzet.getPlaniraniIznos()))
                .odobreniIznos(safe(budzet.getOdobreniIznos()))
                .ukupnoPlaniranoStavke(ukupnoPlanirano)
                .ukupnoStvarnoStavke(ukupnoStvarno)
                .preostaloZaPlaniranje(safe(budzet.getOdobreniIznos()).subtract(ukupnoPlanirano))
                .status(budzet.getStatus())
                .createdAt(budzet.getCreatedAt())
                .updatedAt(budzet.getUpdatedAt())
                .zatvorenAt(budzet.getZatvorenAt())
                .stavke(stavke)
                .build();
    }

    private StavkaBudzetaDto toStavkaDto(StavkaBudzeta stavka) {
        BudzetAlertDto alert = toAlertDto(stavka);
        return StavkaBudzetaDto.builder()
                .budzetId(stavka.getBudzet().getBudzetId())
                .kategorijaId(stavka.getKategorija().getKategorijaId())
                .kategorijaNaziv(stavka.getKategorija().getNaziv())
                .kategorijaOpis(stavka.getKategorija().getOpis())
                .planiraniIznos(safe(stavka.getPlaniraniIznos()))
                .stvarniIznos(safe(stavka.getStvarniIznos()))
                .pragUpozorenja(stavka.getPragUpozorenja())
                .pragKriticnog(stavka.getPragKriticnog())
                .statusKontrole(stavka.getStatusKontrole())
                .alertLevel(alert.getAlertLevel())
                .iskoriscenost(alert.getIskoriscenost())
                .alertPoruka(alert.getPoruka())
                .komentar(stavka.getKomentar())
                .build();
    }

    private BudzetAlertDto toAlertDto(StavkaBudzeta stavka) {
        BudzetAlertDto alert = budzetAlertService.toDto(
                stavka.getBudzet().getBudzetId(),
                stavka.getKategorija().getKategorijaId(),
                stavka.getKategorija().getNaziv(),
                safe(stavka.getStvarniIznos()),
                safe(stavka.getPlaniraniIznos()),
                stavka.getPragUpozorenja(),
                stavka.getPragKriticnog()
        );
        alert.setNazivBudzeta(stavka.getBudzet().getNazivBudzeta());
        alert.setDogadjajId(stavka.getBudzet().getDogadjaj().getDogadjajId());
        alert.setDogadjajNaziv(stavka.getBudzet().getDogadjaj().getNaziv());
        return alert;
    }
}
