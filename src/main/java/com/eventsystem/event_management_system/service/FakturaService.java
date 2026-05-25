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
import com.eventsystem.event_management_system.repository.PlacanjeRepository;
import com.eventsystem.event_management_system.repository.RefundacijaRepository;
import com.eventsystem.event_management_system.repository.StavkaBudzetaRepository;
import com.eventsystem.event_management_system.repository.StavkaFaktureRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.BudzetStatus;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.PlacanjeStatus;
import com.eventsystem.event_management_system.utils.enums.RefundacijaStatus;
import com.eventsystem.event_management_system.utils.enums.TipTroska;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FakturaService {

    private final FakturaRepository fakturaRepository;
    private final PlacanjeRepository placanjeRepository;
    private final RefundacijaRepository refundacijaRepository;
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
        if (dto == null || dto.getTip() == null) {
            throw new BadRequestException("Tip fakture je obavezan.");
        }
        return switch (dto.getTip()) {
            case ULAZNA -> createIncoming(dto);
            case IZLAZNA -> createOutgoing(dto);
        };
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public FakturaDto createIncoming(FakturaDto dto) {
        validateCreateDto(dto);
        if (dto.getDobavljacId() == null) {
            throw new BadRequestException("Dobavljač je obavezan za ulaznu fakturu.");
        }
        if (dto.getKlijentId() != null) {
            throw new BadRequestException("Ulazna faktura ne sme imati klijenta.");
        }
        return saveDraftFaktura(dto, TipFakture.ULAZNA, null, dto.getDobavljacId());
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public FakturaDto createOutgoing(FakturaDto dto) {
        validateCreateDto(dto);
        if (dto.getKlijentId() == null) {
            throw new BadRequestException("Klijent je obavezan za izlaznu fakturu.");
        }
        if (dto.getDobavljacId() != null) {
            throw new BadRequestException("Izlazna faktura ne sme imati dobavljača.");
        }
        return saveDraftFaktura(dto, TipFakture.IZLAZNA, dto.getKlijentId(), null);
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public StavkaFaktureDto addStavka(Long fakturaId, StavkaFaktureDto dto) {
        Faktura f = findEntity(fakturaId);
        if (f.getStatus() != FakturaStatus.DRAFT) {
            throw new BadRequestException("Stavke se mogu dodavati samo dok je faktura u DRAFT statusu.");
        }
        StavkaBudzeta stavkaBudzeta = validateStavkaBudzeta(f, dto);
        BigDecimal jedinicnaCena = requireNonNegative(dto.getJedinicnaCena(), "Jedinična cena");
        BigDecimal kolicina = requirePositiveDecimal(dto.getKolicina(), "Količina");
        BigDecimal ukupnaCena = jedinicnaCena
                .multiply(kolicina)
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
                    .refundiraniIznos(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN))
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

        recomputeUkupanIznos(fakturaId);

        return toStavkaDto(s);
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public void deleteStavka(Long fakturaId, Long stavkaId) {
        Faktura faktura = findEntity(fakturaId);
        if (faktura.getStatus() != FakturaStatus.DRAFT) {
            throw new BadRequestException("Stavke se mogu brisati samo dok je faktura u DRAFT statusu.");
        }

        StavkaFakture stavka = stavkaFaktureRepository.findById(stavkaId)
                .orElseThrow(() -> new NotFoundException("Stavka fakture nije pronađena sa id: " + stavkaId));
        Long stavkaFakturaId = stavka.getFaktura() != null ? stavka.getFaktura().getFakturaId() : null;
        if (!fakturaId.equals(stavkaFakturaId)) {
            throw new BadRequestException("Stavka ne pripada izabranoj fakturi.");
        }

        Long budzetId = stavka.getBudzetId();
        Long kategorijaId = stavka.getKategorijaId();
        if (faktura.getTip() == TipFakture.ULAZNA && stavka.getTrosakId() != null) {
            trosakRepository.findById(stavka.getTrosakId()).ifPresent(trosak -> {
                trosak.setRefundiraniIznos(safe(trosak.getIznos()).setScale(2, RoundingMode.HALF_EVEN));
                trosakRepository.save(trosak);
            });
        }

        stavkaFaktureRepository.delete(stavka);
        stavkaFaktureRepository.flush();
        recomputeUkupanIznos(fakturaId);

        if (faktura.getTip() == TipFakture.ULAZNA && budzetId != null && kategorijaId != null) {
            budzetService.recomputeStavku(budzetId, kategorijaId);
        }
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public FakturaDto issue(Long fakturaId) {
        Faktura f = findEntity(fakturaId);
        if (f.getStatus() != FakturaStatus.DRAFT) {
            throw new BadRequestException("Samo DRAFT faktura može biti izdata.");
        }
        if (stavkaFaktureRepository.countByFakturaId(fakturaId) < 1) {
            throw new BadRequestException("Faktura mora imati bar jednu stavku.");
        }

        recomputeUkupanIznos(fakturaId);
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
        if (safe(f.getPlaceniIznos()).compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException("Faktura se ne može otkazati dok ima nepovraćenih plaćanja.");
        }

        if (f.getTip() == TipFakture.ULAZNA) {
            var lista = trosakRepository.findByFakturaIdAndTip(f.getFakturaId(), TipTroska.AUTO_ULAZNA);
            if (lista != null && !lista.isEmpty()) {
                Set<String> recomputeKeys = new HashSet<>();
                for (Trosak t : lista) {
                    t.setRefundiraniIznos(safe(t.getIznos()).setScale(2, RoundingMode.HALF_EVEN));
                    if (t.getBudzetId() != null && t.getKategorijaId() != null) {
                        recomputeKeys.add(t.getBudzetId() + ":" + t.getKategorijaId());
                    }
                }
                trosakRepository.saveAll(lista);
                recomputeKeys.forEach(this::recomputeStavkaKey);
            }
        }

        f.setStatus(FakturaStatus.OTKAZANA);
        fakturaRepository.save(f);
    }

    @Transactional
    public void recomputePlaceniIznos(Faktura faktura) {
        BigDecimal bruto = safe(placanjeRepository.sumByFakturaIdAndStatus(
                faktura.getFakturaId(),
                PlacanjeStatus.COMPLETED
        ));
        BigDecimal refundirano = safe(refundacijaRepository.sumByFakturaIdAndStatus(
                faktura.getFakturaId(),
                RefundacijaStatus.IZVRSENA
        ));

        BigDecimal placeno = bruto.subtract(refundirano);
        if (placeno.compareTo(BigDecimal.ZERO) < 0) {
            placeno = BigDecimal.ZERO;
        }
        faktura.setPlaceniIznos(placeno.setScale(2, RoundingMode.HALF_EVEN));
        fakturaRepository.save(faktura);
    }

    @Transactional
    public void autoUpdateStatus(Faktura faktura) {
        if (faktura.getStatus() == FakturaStatus.DRAFT || faktura.getStatus() == FakturaStatus.OTKAZANA) {
            return;
        }

        BigDecimal placeni = safe(faktura.getPlaceniIznos()).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal ukupno = safe(faktura.getUkupnaIznos()).setScale(2, RoundingMode.HALF_EVEN);

        if (placeni.compareTo(BigDecimal.ZERO) == 0) {
            faktura.setStatus(FakturaStatus.IZDATA);
        } else if (placeni.compareTo(ukupno) < 0) {
            faktura.setStatus(FakturaStatus.DELIMICNO_PLACENA);
        } else if (placeni.compareTo(ukupno) >= 0) {
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
                .klijentId(e.getKlijentId())
                .dobavljacId(e.getDobavljacId())
                .ugovorId(e.getUgovorId())
                .kreiraoId(e.getKreiraoId())
                .rokPlacanja(e.getRokPlacanja())
                .napomena(e.getNapomena())
                .kreiranAt(e.getKreiranAt())
                .stavke(e.getStavke().stream().map(this::toStavkaDto).toList())
                    .placanja(e.getPlacanja().stream().map(p -> PlacanjeDto.builder()
                        .placanjeId(p.getPlacanjeId())
                        .fakturaId(e.getFakturaId())
                        .iznos(p.getIznos())
                        .referentniBroj(p.getReferentniBroj())
                        .datumPlacanja(p.getDatumPlacanja())
                        .metod(p.getMetod())
                        .status(p.getStatus())
                        .napomena(p.getNapomena())
                        .dokumentUrl(p.getDokumentUrl())
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
    }

    private StavkaFaktureDto toStavkaDto(StavkaFakture stavka) {
        return StavkaFaktureDto.builder()
                .stavkaFaktureId(stavka.getStavkaFaktureId())
                .fakturaId(stavka.getFaktura() != null ? stavka.getFaktura().getFakturaId() : null)
                .redniBroj(stavka.getRedniBroj())
                .naziv(stavka.getNaziv())
                .kolicina(stavka.getKolicina())
                .jedinicnaCena(stavka.getJedinicnaCena())
                .ukupnaCena(stavka.getUkupnaCena())
                .napomena(stavka.getNapomena())
                .budzetId(stavka.getBudzetId())
                .kategorijaId(stavka.getKategorijaId())
                .trosakId(stavka.getTrosakId())
                .build();
    }

    private FakturaDto saveDraftFaktura(FakturaDto dto, TipFakture tip, Long klijentId, Long dobavljacId) {
        Long kreiraoId = currentUserService.getCurrentZaposleni().getKorisnikId();

        Faktura f = Faktura.builder()
                .brojFakture(dto.getBrojFakture().trim())
                .tip(tip)
                .status(FakturaStatus.DRAFT)
                .ukupnaIznos(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN))
                .placeniIznos(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN))
                .datumIzdavanja(dto.getDatumIzdavanja())
                .dogadjajId(dto.getDogadjajId())
                .klijentId(klijentId)
                .dobavljacId(dobavljacId)
                .ugovorId(dto.getUgovorId())
                .kreiraoId(kreiraoId)
                .rokPlacanja(dto.getRokPlacanja())
                .napomena(dto.getNapomena())
                .build();

        return toDto(fakturaRepository.save(f));
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

    private BigDecimal requirePositiveDecimal(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " je obavezna.");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(fieldName + " mora biti veća od 0.");
        }
        return value.setScale(3, RoundingMode.HALF_EVEN);
    }

    private BigDecimal sumStavke(Long fakturaId) {
        return stavkaFaktureRepository.sumUkupnaCenaByFakturaId(fakturaId)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    private void recomputeUkupanIznos(Long fakturaId) {
        Faktura faktura = findEntity(fakturaId);
        faktura.setUkupnaIznos(sumStavke(fakturaId));
        fakturaRepository.save(faktura);
    }

    private void recomputeStavkaKey(String key) {
        String[] parts = key.split(":");
        budzetService.recomputeStavku(Long.valueOf(parts[0]), Long.valueOf(parts[1]));
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " je obavezan.");
        }
        return value.trim();
    }
}
