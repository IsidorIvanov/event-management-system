package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.*;
import com.eventsystem.event_management_system.model.*;
import com.eventsystem.event_management_system.model.compositePK.StavkaPorudzbeniceId;
import com.eventsystem.event_management_system.repository.PorudzbenicaRepository;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import com.eventsystem.event_management_system.utils.enums.StatusPorudzbenice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class PorudzbenicaService {

    private final PorudzbenicaRepository porudzbenicaRepository;
    private final NabavkaService nabavkaService;

    @Transactional(readOnly = true)
    public List<PorudzbenicaDto> getAll() {
        return porudzbenicaRepository.findAllWithDetalji().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PorudzbenicaDto> getByDogadjaj(Long dogadjajId) {
        return porudzbenicaRepository.findByDogadjajId(dogadjajId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PorudzbenicaDto getById(Long id) {
        return toDto(findEntityWithDetalji(id));
    }

    @Transactional
    public PorudzbenicaDto generisiIzNabavke(GenerisiPorudzbenicuRequestDto request) {
        Nabavka nabavka = nabavkaService.findEntityWithDetalji(request.getNabavkaId());

        if (porudzbenicaRepository.existsByNabavkaNabavkaId(request.getNabavkaId())) {
            throw new RuntimeException("Porudžbenica za ovu nabavku već postoji.");
        }
        if (nabavka.getDobavljac() == null) {
            throw new RuntimeException("Nabavka nema dodeljenog dobavljača. Prvo pokrenite selekciju.");
        }
        if (nabavka.getStavke() == null || nabavka.getStavke().isEmpty()) {
            throw new RuntimeException("Nabavka nema stavki za generisanje porudžbenice.");
        }

        Porudzbenica porudzbenica = Porudzbenica.builder()
                .nabavka(nabavka)
                .dobavljac(nabavka.getDobavljac())
                .brojPorudzbenice(generisiBrojPorudzbenice())
                .status(StatusPorudzbenice.KREIRANA)
                .rokIsporuke(request.getRokIsporuke())
                .napomena(request.getNapomena())
                .stavke(new ArrayList<>())
                .build();

        Porudzbenica saved = porudzbenicaRepository.saveAndFlush(porudzbenica);
        kopirajStavkeIzNabavke(saved, nabavka);
        recalculateTotal(saved);
        saved = porudzbenicaRepository.saveAndFlush(saved);

        nabavkaService.updateStatus(request.getNabavkaId(), StatusNabavke.ZAVRSENA);

        return getById(saved.getPorudzbenicaId());
    }

    @Transactional
    public PorudzbenicaDto updateStatus(Long id, StatusPorudzbenice status) {
        Porudzbenica p = findEntityWithDetalji(id);
        p.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case POSLATA -> p.setPoslataAt(now);
            case POTVRDJENA -> p.setPotvrdjenaAt(now);
            case ISPORUCENA -> p.setIsporucenaAt(now);
            default -> { }
        }
        return toDto(porudzbenicaRepository.save(p));
    }

    public Porudzbenica findEntityWithDetalji(Long id) {
        return porudzbenicaRepository.findByIdWithDetalji(id)
                .orElseThrow(() -> new RuntimeException("Porudžbenica nije pronadjena sa id: " + id));
    }

    private void kopirajStavkeIzNabavke(Porudzbenica porudzbenica, Nabavka nabavka) {
        AtomicInteger rb = new AtomicInteger(1);
        for (StavkaNabavke sn : nabavka.getStavke()) {
            StavkaPorudzbenice stavka = StavkaPorudzbenice.builder()
                    .id(new StavkaPorudzbeniceId(porudzbenica.getPorudzbenicaId(), rb.getAndIncrement()))
                    .porudzbenica(porudzbenica)
                    .nazivResursa(sn.getNazivResursa())
                    .kolicina(sn.getKolicina())
                    .jedinicnaCena(sn.getJedinicnaCena())
                    .ukupnaCena(sn.getUkupnaCena())
                    .razlogPotrebe("Stavka iz nabavke #" + nabavka.getNabavkaId())
                    .build();
            porudzbenica.getStavke().add(stavka);
        }
    }

    private void recalculateTotal(Porudzbenica p) {
        BigDecimal total = p.getStavke().stream()
                .map(StavkaPorudzbenice::getUkupnaCena)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        p.setUkupnaCena(total);
    }

    private String generisiBrojPorudzbenice() {
        String prefix = "PO-" + Year.now().getValue() + "-";
        long count = porudzbenicaRepository.countByBrojPorudzbeniceStartingWith(prefix);
        return prefix + String.format("%04d", count + 1);
    }

    private PorudzbenicaDto toDto(Porudzbenica entity) {
        return PorudzbenicaDto.builder()
                .porudzbenicaId(entity.getPorudzbenicaId())
                .nabavkaId(entity.getNabavka().getNabavkaId())
                .dogadjajId(entity.getNabavka().getDogadjaj().getDogadjajId())
                .dogadjajNaziv(entity.getNabavka().getDogadjaj().getNaziv())
                .dobavljacId(entity.getDobavljac().getDobavljacId())
                .dobavljacNaziv(entity.getDobavljac().getNaziv())
                .brojPorudzbenice(entity.getBrojPorudzbenice())
                .status(entity.getStatus())
                .ukupnaCena(entity.getUkupnaCena())
                .kreiranAt(entity.getKreiranAt())
                .poslataAt(entity.getPoslataAt())
                .potvrdjenaAt(entity.getPotvrdjenaAt())
                .rokIsporuke(entity.getRokIsporuke())
                .isporucenaAt(entity.getIsporucenaAt())
                .napomena(entity.getNapomena())
                .stavke(entity.getStavke().stream().map(this::toStavkaDto).toList())
                .build();
    }

    private StavkaPorudzbeniceDto toStavkaDto(StavkaPorudzbenice s) {
        return StavkaPorudzbeniceDto.builder()
                .redniBroj(s.getId().getRedniBroj())
                .nazivResursa(s.getNazivResursa())
                .kolicina(s.getKolicina())
                .jedinicnaCena(s.getJedinicnaCena())
                .ukupnaCena(s.getUkupnaCena())
                .razlogPotrebe(s.getRazlogPotrebe())
                .build();
    }
}
