package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.FakturaDto;
import com.eventsystem.event_management_system.dto.StavkaFaktureDto;
import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.model.StavkaFakture;
import com.eventsystem.event_management_system.model.Trosak;
import com.eventsystem.event_management_system.repository.FakturaRepository;
import com.eventsystem.event_management_system.repository.StavkaFaktureRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.TipTroska;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FakturaService {

    private final FakturaRepository fakturaRepository;
    private final StavkaFaktureRepository stavkaFaktureRepository;
    private final TrosakRepository trosakRepository;

    @Transactional(readOnly = true)
    public List<FakturaDto> getAll() {
        return fakturaRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Faktura findEntity(Long id) {
        return fakturaRepository.findById(id).orElseThrow(() -> new RuntimeException("Faktura not found"));
    }

    @Transactional
    public FakturaDto create(FakturaDto dto) {
        Faktura f = Faktura.builder()
                .brojFakture(dto.getBrojFakture())
                .tip(dto.getTip())
                .status(dto.getStatus())
                .ukupnaIznos(dto.getUkupnaIznos())
                .placeniIznos(dto.getPlaceniIznos())
                .datumIzdavanja(dto.getDatumIzdavanja())
            .dogadjajId(dto.getDogadjajId())
            .dobavljacId(dto.getDobavljacId())
            .kreiraoId(dto.getKreiraoId())
                .rokPlacanja(dto.getRokPlacanja())
                .napomena(dto.getNapomena())
                .build();

        if (dto.getStavke() != null) {
            for (StavkaFaktureDto s : dto.getStavke()) {
                StavkaFakture stavka = StavkaFakture.builder()
                        .redniBroj(s.getRedniBroj())
                        .naziv(s.getNaziv())
                        .kolicina(s.getKolicina())
                        .jedinicnaCena(s.getJedinicnaCena())
                        .ukupnaCena(s.getUkupnaCena())
                        .napomena(s.getNapomena())
                    .budzetId(s.getBudzetId())
                    .kategorijaId(s.getKategorijaId())
                        .faktura(f)
                        .build();
                f.getStavke().add(stavka);
            }
        }

        Faktura saved = fakturaRepository.save(f);
        recomputePlaceniIznos(saved);
        return toDto(saved);
    }

    @Transactional
    public StavkaFakture addStavka(Long fakturaId, StavkaFaktureDto dto) {
        Faktura f = findEntity(fakturaId);
        StavkaFakture s = StavkaFakture.builder()
                .faktura(f)
                .redniBroj(dto.getRedniBroj())
                .naziv(dto.getNaziv())
                .kolicina(dto.getKolicina())
                .jedinicnaCena(dto.getJedinicnaCena())
                .ukupnaCena(dto.getUkupnaCena())
                .napomena(dto.getNapomena())
            .budzetId(dto.getBudzetId())
            .kategorijaId(dto.getKategorijaId())
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
        }

        // update faktura ukupna cena
        BigDecimal ukupno = f.getStavke().stream()
                .map(StavkaFakture::getUkupnaCena)
                .filter(x -> x != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        f.setUkupnaIznos(ukupno);
        fakturaRepository.save(f);

        return s;
    }

    @Transactional
    public void cancelFaktura(Long fakturaId) {
        Faktura f = findEntity(fakturaId);
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
                .build();
    }
}
