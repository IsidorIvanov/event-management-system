package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.FakturaDto;
import com.eventsystem.event_management_system.dto.PlacanjeDto;
import com.eventsystem.event_management_system.dto.RefundacijaDto;
import com.eventsystem.event_management_system.dto.StavkaFaktureDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Budzet;
import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.model.StavkaBudzeta;
import com.eventsystem.event_management_system.model.StavkaFakture;
import com.eventsystem.event_management_system.model.Trosak;
import com.eventsystem.event_management_system.model.compositePK.StavkaBudzetaId;
import com.eventsystem.event_management_system.repository.FakturaRepository;
import com.eventsystem.event_management_system.repository.StavkaBudzetaRepository;
import com.eventsystem.event_management_system.repository.StavkaFaktureRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.BudzetStatus;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.TipTroska;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
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
public class FakturaService {

    private final FakturaRepository fakturaRepository;
    private final StavkaFaktureRepository stavkaFaktureRepository;
    private final StavkaBudzetaRepository stavkaBudzetaRepository;
    private final TrosakRepository trosakRepository;
    private final BudzetService budzetService;
    private final CurrentUserService currentUserService;

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public List<FakturaDto> getAll() {
        return fakturaRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Faktura findEntity(Long id) {
        return fakturaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Faktura nije pronađena sa id: " + id));
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public FakturaDto getById(Long id) {
        return toDto(findEntity(id));
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public FakturaDto create(FakturaDto dto) {
        validateCreateDto(dto);
        Long kreiraoId = currentUserService.getCurrentZaposleni().getKorisnikId();

        Faktura f = Faktura.builder()
                .brojFakture(dto.getBrojFakture().trim())
                .tip(dto.getTip())
                .status(FakturaStatus.DRAFT)
                .ukupnaIznos(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN))
                .placeniIznos(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN))
                .datumIzdavanja(dto.getDatumIzdavanja())
                .dogadjajId(dto.getDogadjajId())
                .dobavljacId(dto.getDobavljacId())
                .kreiraoId(kreiraoId)
                .rokPlacanja(dto.getRokPlacanja())
                .napomena(dto.getNapomena())
                .build();

        Faktura saved = fakturaRepository.save(f);
        return toDto(saved);
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public StavkaFakture addStavka(Long fakturaId, StavkaFaktureDto dto) {
        Faktura f = findEntity(fakturaId);
        if (f.getStatus() != FakturaStatus.DRAFT) {
            throw new BadRequestException("Stavke se mogu dodavati samo dok je faktura u DRAFT statusu.");
        }
        StavkaBudzeta stavkaBudzeta = validateStavkaBudzeta(f, dto);
        BigDecimal jedinicnaCena = requireNonNegative(dto.getJedinicnaCena(), "Jedinična cena");
        Integer kolicina = requirePositiveInteger(dto.getKolicina(), "Količina");
        BigDecimal ukupnaCena = jedinicnaCena
                .multiply(BigDecimal.valueOf(kolicina))
                .setScale(2, RoundingMode.HALF_EVEN);

        StavkaFakture s = StavkaFakture.builder()
                .faktura(f)
                .redniBroj(resolveRedniBroj(fakturaId, dto.getRedniBroj()))
                .naziv(normalizeRequired(dto.getNaziv(), "Naziv stavke"))
                .kolicina(kolicina)
                .jedinicnaCena(jedinicnaCena)
                .ukupnaCena(ukupnaCena)
                .napomena(dto.getNapomena())
                .budzetId(stavkaBudzeta.getBudzet().getBudzetId())
                .kategorijaId(stavkaBudzeta.getKategorija().getKategorijaId())
                .build();

        s = stavkaFaktureRepository.save(s);

        // Flow 7.1: ako je ulazna faktura, kreiraj AUTO_ULAZNA trosak
        if (f.getTip() == TipFakture.ULAZNA) {
            Trosak t = Trosak.builder()
                    .opis(s.getNaziv())
                    .iznos(s.getUkupnaCena())
                    .tip(TipTroska.AUTO_ULAZNA)
                    .fakturaId(f.getFakturaId())
                    .stavkaFaktureId(s.getStavkaFaktureId())
                    .budzetId(s.getBudzetId())
                    .kategorijaId(s.getKategorijaId())
                    .dogadjajId(f.getDogadjajId())
                    .dobavljacId(f.getDobavljacId())
                    .evidentiraoId(f.getKreiraoId())
                    .datumTroska(f.getDatumIzdavanja())
                    .kreiranAt(LocalDateTime.now())
                    .build();
            Trosak novi = trosakRepository.save(t);

            // set reference back to stavka
            s.setTrosakId(novi.getTrosakId());
            stavkaFaktureRepository.save(s);
            budzetService.recomputeStavku(s.getBudzetId(), s.getKategorijaId());
        }

        f.setUkupnaIznos(sumStavke(fakturaId));
        fakturaRepository.save(f);

        return s;
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public FakturaDto issue(Long fakturaId) {
        Faktura f = findEntity(fakturaId);
        if (f.getStatus() != FakturaStatus.DRAFT) {
            throw new RuntimeException("Samo DRAFT faktura može biti izdata");
        }
        if (f.getStavke() == null || f.getStavke().isEmpty()) {
            throw new RuntimeException("Faktura mora imati bar jednu stavku");
        }

        BigDecimal ukupno = sumStavke(fakturaId);

        f.setUkupnaIznos(ukupno);
        f.setStatus(FakturaStatus.IZDATA);
        fakturaRepository.save(f);
        return toDto(f);
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public void cancelFaktura(Long fakturaId) {
        Faktura f = findEntity(fakturaId);
        if (f.getStatus() == FakturaStatus.OTKAZANA) {
            throw new BadRequestException("Faktura je već otkazana.");
        }
        if (f.getPlaceniIznos() != null && f.getPlaceniIznos().compareTo(BigDecimal.ZERO) != 0) {
            throw new RuntimeException("Faktura se ne može otkazati dok ima nepovraćenih plaćanja");
        }

        // Flow 7.3: za ulazne fakture, ako placeni_iznos == 0 ukloni AUTO_ULAZNA troskove
        if (f.getTip() == TipFakture.ULAZNA && f.getPlaceniIznos() != null && f.getPlaceniIznos().compareTo(BigDecimal.ZERO) == 0) {
            var lista = trosakRepository.findByFakturaIdAndTip(f.getFakturaId(), TipTroska.AUTO_ULAZNA);
            if (lista != null && !lista.isEmpty()) {
                for (Trosak t : lista) {
                    t.setRefundiraniIznos(t.getIznos());
                }
                trosakRepository.saveAll(lista);
                lista.forEach(t -> budzetService.recomputeStavku(t.getBudzetId(), t.getKategorijaId()));
            }
        }

        f.setStatus(FakturaStatus.OTKAZANA);
        fakturaRepository.save(f);
    }

    @Transactional
    public void recomputePlaceniIznos(Faktura faktura) {
        // sum completed payments
        BigDecimal sumPayments = faktura.getPlacanja().stream()
            .filter(p -> p.getStatus() != null && p.getStatus() == com.eventsystem.event_management_system.utils.enums.PlacanjeStatus.COMPLETED)
            .map(p -> p.getIznos())
            .filter(x -> x != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // sum executed refunds collected through payments
        BigDecimal sumRefunds = faktura.getPlacanja().stream()
            .flatMap(pl -> pl.getRefundacije().stream())
            .filter(rf -> rf.getStatus() != null && rf.getStatus() == com.eventsystem.event_management_system.utils.enums.RefundacijaStatus.IZVRSENA)
            .map(rf -> rf.getIznos())
            .filter(x -> x != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal novi = sumPayments.subtract(sumRefunds);
        if (novi.compareTo(BigDecimal.ZERO) < 0) novi = BigDecimal.ZERO;
        faktura.setPlaceniIznos(novi);
        fakturaRepository.save(faktura);
        autoUpdateStatus(faktura);
    }

    @Transactional
    public void autoUpdateStatus(Faktura faktura) {
        if (faktura.getStatus() == FakturaStatus.DRAFT || faktura.getStatus() == FakturaStatus.OTKAZANA) {
            return;
        }

        BigDecimal placeni = faktura.getPlaceniIznos() == null ? BigDecimal.ZERO : faktura.getPlaceniIznos();
        BigDecimal ukupno = faktura.getUkupnaIznos() == null ? BigDecimal.ZERO : faktura.getUkupnaIznos();

        if (placeni.compareTo(BigDecimal.ZERO) == 0) {
            faktura.setStatus(FakturaStatus.IZDATA);
        } else if (placeni.compareTo(ukupno) < 0) {
            faktura.setStatus(FakturaStatus.DELIMICNO_PLACENA);
        } else if (placeni.compareTo(ukupno) == 0) {
            faktura.setStatus(FakturaStatus.PLACENA);
        }

        fakturaRepository.save(faktura);
    }

    @Transactional(readOnly = true)
    public FakturaDto toDto(Faktura e) {
        return FakturaDto.builder()
                .fakturaId(e.getFakturaId())
                .brojFakture(e.getBrojFakture())
                .tip(e.getTip())
                .status(e.getStatus())
                .ukupnaIznos(e.getUkupnaIznos())
                .placeniIznos(e.getPlaceniIznos())
                .datumIzdavanja(e.getDatumIzdavanja())
                .dogadjajId(e.getDogadjajId())
                .dobavljacId(e.getDobavljacId())
                .kreiraoId(e.getKreiraoId())
                .rokPlacanja(e.getRokPlacanja())
                .napomena(e.getNapomena())
                .kreiranAt(e.getKreiranAt())
                .stavke(e.getStavke().stream().map(s -> StavkaFaktureDto.builder()
                        .stavkaFaktureId(s.getStavkaFaktureId())
                        .redniBroj(s.getRedniBroj())
                        .naziv(s.getNaziv())
                        .kolicina(s.getKolicina())
                        .jedinicnaCena(s.getJedinicnaCena())
                        .ukupnaCena(s.getUkupnaCena())
                        .napomena(s.getNapomena())
                        .budzetId(s.getBudzetId())
                        .kategorijaId(s.getKategorijaId())
                        .build()).toList())
                    .placanja(e.getPlacanja().stream().map(p -> PlacanjeDto.builder()
                        .placanjeId(p.getPlacanjeId())
                        .fakturaId(e.getFakturaId())
                        .iznos(p.getIznos())
                        .metod(p.getMetod())
                        .status(p.getStatus())
                        .napomena(p.getNapomena())
                        .kreiranoAt(p.getKreiranoAt())
                        .potvrdjenoAt(p.getPotvrdjenoAt())
                        .refundacije(p.getRefundacije().stream().map(r -> RefundacijaDto.builder()
                            .refundacijaId(r.getRefundacijaId())
                            .placanjeId(p.getPlacanjeId())
                            .iznos(r.getIznos())
                            .razlog(r.getRazlog())
                            .status(r.getStatus())
                            .kreiraoId(r.getKreiraoId())
                            .odobrioId(r.getOdobrioId())
                            .izvrsenoAt(r.getIzvrsenoAt())
                            .kreiranoAt(r.getKreiranoAt())
                            .build()).toList())
                        .build()).toList())
                .build();
    }

    private void validateCreateDto(FakturaDto dto) {
        if (dto == null) {
            throw new BadRequestException("Podaci za fakturu su obavezni.");
        }
        String brojFakture = normalizeRequired(dto.getBrojFakture(), "Broj fakture");
        if (fakturaRepository.findByBrojFakture(brojFakture) != null) {
            throw new BadRequestException("Faktura sa ovim brojem već postoji.");
        }
        if (dto.getTip() == null) {
            throw new BadRequestException("Tip fakture je obavezan.");
        }
        if (dto.getDogadjajId() == null) {
            throw new BadRequestException("Događaj je obavezan.");
        }
        if (dto.getDatumIzdavanja() == null) {
            throw new BadRequestException("Datum izdavanja je obavezan.");
        }
        if (dto.getRokPlacanja() == null) {
            throw new BadRequestException("Rok plaćanja je obavezan.");
        }
        if (dto.getRokPlacanja().isBefore(dto.getDatumIzdavanja())) {
            throw new BadRequestException("Rok plaćanja ne može biti pre datuma izdavanja.");
        }
        if (dto.getTip() == TipFakture.ULAZNA && dto.getDobavljacId() == null) {
            throw new BadRequestException("Dobavljač je obavezan za ulaznu fakturu.");
        }
        if (dto.getTip() == TipFakture.IZLAZNA && dto.getDobavljacId() != null) {
            throw new BadRequestException("Izlazna faktura ne sme imati dobavljača.");
        }
    }

    private StavkaBudzeta validateStavkaBudzeta(Faktura faktura, StavkaFaktureDto dto) {
        if (dto == null) {
            throw new BadRequestException("Podaci za stavku fakture su obavezni.");
        }
        if (dto.getBudzetId() == null || dto.getKategorijaId() == null) {
            throw new BadRequestException("Budžet i kategorija su obavezni za stavku fakture.");
        }
        StavkaBudzeta stavka = stavkaBudzetaRepository
                .findById(new StavkaBudzetaId(dto.getBudzetId(), dto.getKategorijaId()))
                .orElseThrow(() -> new BadRequestException("Izabrana budžetska stavka ne postoji."));

        Budzet budzet = stavka.getBudzet();
        if (budzet.getStatus() == BudzetStatus.CLOSED) {
            throw new BadRequestException("Zatvoren budžet se ne može teretiti novom stavkom fakture.");
        }
        if (!budzet.getDogadjaj().getDogadjajId().equals(faktura.getDogadjajId())) {
            throw new BadRequestException("Budžet mora pripadati istom događaju kao faktura.");
        }
        return stavka;
    }

    private Integer resolveRedniBroj(Long fakturaId, Integer requestedRedniBroj) {
        if (requestedRedniBroj != null) {
            if (requestedRedniBroj <= 0) {
                throw new BadRequestException("Redni broj stavke mora biti veći od 0.");
            }
            return requestedRedniBroj;
        }
        return stavkaFaktureRepository.maxRedniBrojByFakturaId(fakturaId) + 1;
    }

    private BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " je obavezna.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(fieldName + " ne može biti negativna.");
        }
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    private Integer requirePositiveInteger(Integer value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " je obavezna.");
        }
        if (value <= 0) {
            throw new BadRequestException(fieldName + " mora biti veća od 0.");
        }
        return value;
    }

    private BigDecimal sumStavke(Long fakturaId) {
        return stavkaFaktureRepository.sumUkupnaCenaByFakturaId(fakturaId)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " je obavezan.");
        }
        return value.trim();
    }
}
