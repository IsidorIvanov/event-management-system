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
    public List<PorudzbenicaDto> generisiIzNabavke(GenerisiPorudzbenicuRequestDto request) {
        Nabavka nabavka = nabavkaService.findEntityWithDetalji(request.getNabavkaId());

        if (nabavka.getStavke() == null || nabavka.getStavke().isEmpty()) {
            throw new RuntimeException("Nabavka nema stavki za generisanje porudžbenice.");
        }

        var grupe = nabavka.getStavke().stream()
                .filter(s -> s.getCenovnik() != null && s.getCenovnik().getDobavljac() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        s -> s.getCenovnik().getDobavljac().getDobavljacId()));

        if (grupe.isEmpty()) {
            if (nabavka.getDobavljac() == null) {
                throw new RuntimeException("Nabavka nema dodeljenog dobavljača. Prvo pokrenite selekciju ili alokaciju.");
            }
            if (porudzbenicaRepository.existsByNabavkaNabavkaIdAndDobavljacDobavljacId(
                    request.getNabavkaId(), nabavka.getDobavljac().getDobavljacId())) {
                throw new RuntimeException("Porudžbenica za ovu nabavku i dobavljača već postoji.");
            }
            return List.of(kreirajPorudzbenicu(nabavka, nabavka.getDobavljac(), nabavka.getStavke(), request));
        }

        List<PorudzbenicaDto> kreirane = new ArrayList<>();
        for (var entry : grupe.entrySet()) {
            if (porudzbenicaRepository.existsByNabavkaNabavkaIdAndDobavljacDobavljacId(
                    request.getNabavkaId(), entry.getKey())) {
                continue;
            }
            Dobavljac dobavljac = entry.getValue().getFirst().getCenovnik().getDobavljac();
            kreirane.add(kreirajPorudzbenicu(nabavka, dobavljac, entry.getValue(), request));
        }

        if (kreirane.isEmpty()) {
            throw new RuntimeException("Porudžbenice za sve dobavljače u nabavci već postoje.");
        }
        return kreirane;
    }

    private PorudzbenicaDto kreirajPorudzbenicu(
            Nabavka nabavka,
            Dobavljac dobavljac,
            List<StavkaNabavke> stavkeNabavke,
            GenerisiPorudzbenicuRequestDto request
    ) {
        Porudzbenica porudzbenica = Porudzbenica.builder()
                .nabavka(nabavka)
                .dobavljac(dobavljac)
                .brojPorudzbenice(generisiBrojPorudzbenice())
                .status(StatusPorudzbenice.KREIRANA)
                .rokIsporuke(request.getRokIsporuke())
                .napomena(request.getNapomena())
                .stavke(new ArrayList<>())
                .build();

        Porudzbenica saved = porudzbenicaRepository.saveAndFlush(porudzbenica);
        kopirajStavke(saved, stavkeNabavke, nabavka.getNabavkaId());
        recalculateTotal(saved);
        saved = porudzbenicaRepository.saveAndFlush(saved);
        syncNabavkaStatus(saved, StatusPorudzbenice.KREIRANA);
        return getById(saved.getPorudzbenicaId());
    }

    @Transactional
    public PorudzbenicaDto updateStatus(Long id, StatusPorudzbenice status) {
        Porudzbenica p = porudzbenicaRepository.findByIdWithNabavka(id)
                .orElseThrow(() -> new RuntimeException("Porudžbenica nije pronadjena sa id: " + id));

        p.setStatus(status);
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case POSLATA -> p.setPoslataAt(now);
            case POTVRDJENA -> p.setPotvrdjenaAt(now);
            case U_ISPORUCI -> { /* status only */ }
            case ISPORUCENA -> p.setIsporucenaAt(now);
            default -> { }
        }

        porudzbenicaRepository.saveAndFlush(p);
        syncNabavkaStatus(p, status);
        return getById(id);
    }

    private void syncNabavkaStatus(Porudzbenica porudzbenica, StatusPorudzbenice statusPorudzbenice) {
        Long nabavkaId = porudzbenica.getNabavka().getNabavkaId();
        StatusNabavke nabavkaStatus = mapPorudzbenicaToNabavka(statusPorudzbenice);
        nabavkaService.updateStatus(nabavkaId, nabavkaStatus);
    }

    private StatusNabavke mapPorudzbenicaToNabavka(StatusPorudzbenice statusPorudzbenice) {
        return switch (statusPorudzbenice) {
            case KREIRANA -> StatusNabavke.PREDLOZENA;
            case POSLATA -> StatusNabavke.U_OBRADI;
            case POTVRDJENA -> StatusNabavke.POTVRDJENA;
            case U_ISPORUCI -> StatusNabavke.U_ISPORUCI;
            case ISPORUCENA -> StatusNabavke.ZAVRSENA;
            case OTKAZANA -> StatusNabavke.ODBIJENA;
        };
    }

    public Porudzbenica findEntityWithDetalji(Long id) {
        return porudzbenicaRepository.findByIdWithDetalji(id)
                .orElseThrow(() -> new RuntimeException("Porudžbenica nije pronadjena sa id: " + id));
    }

    private void kopirajStavke(Porudzbenica porudzbenica, List<StavkaNabavke> stavkeNabavke, Long nabavkaId) {
        AtomicInteger rb = new AtomicInteger(1);
        for (StavkaNabavke sn : stavkeNabavke) {
            StavkaPorudzbenice stavka = StavkaPorudzbenice.builder()
                    .id(new StavkaPorudzbeniceId(porudzbenica.getPorudzbenicaId(), rb.getAndIncrement()))
                    .porudzbenica(porudzbenica)
                    .nazivResursa(sn.getNazivResursa())
                    .kolicina(sn.getKolicina())
                    .jedinicnaCena(sn.getJedinicnaCena())
                    .ukupnaCena(sn.getUkupnaCena())
                    .razlogPotrebe("Stavka iz nabavke #" + nabavkaId)
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
