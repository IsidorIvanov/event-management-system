package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.*;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Sala;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.repository.*;
import com.eventsystem.event_management_system.utils.enums.TipSale;
import com.eventsystem.event_management_system.utils.enums.TipSesije;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlokacijaSaleService {

    private final DogadjajRepository dogadjajRepository;
    private final SesijaRepository sesijaRepository;
    private final SalaRepository salaRepository;
    private final PotrebaOpremeDetekcijaService potrebaOpremeDetekcijaService;
    private final OpremaService opremaService;
    private final DinamickoCeneService dinamickoCeneService;

    @Transactional(readOnly = true)
    public PredlogSalaResponseDto predlogZaSesiju(Long sesijaId) {
        Sesija sesija = sesijaRepository.findById(sesijaId)
                .orElseThrow(() -> new NotFoundException("Sesija nije pronađena sa id: " + sesijaId));

        PredlogSalaRequestDto request = PredlogSalaRequestDto.builder()
                .dogadjajId(sesija.getDogadjaj().getDogadjajId())
                .sesijaId(sesijaId)
                .lokacijaId(sesija.getSala().getId().getLokacijaId())
                .datum(sesija.getDatum())
                .vremePocetka(sesija.getVremePocetka())
                .vremeZavrsetka(sesija.getVremeZavrsetka())
                .tip(sesija.getTip())
                .kapacitet(sesija.getKapacitet())
                .build();

        return predlogZaParametre(request);
    }

    @Transactional(readOnly = true)
    public PredlogSalaResponseDto predlogZaParametre(PredlogSalaRequestDto request) {
        Dogadjaj dogadjaj = dogadjajRepository.findById(request.getDogadjajId())
                .orElseThrow(() -> new NotFoundException("Događaj nije pronađen sa id: " + request.getDogadjajId()));

        List<Sala> sale = salaRepository.findByLokacija_LokacijaId(request.getLokacijaId());
        List<PredlogSalaDto> predlozi = new ArrayList<>();

        for (Sala sala : sale) {
            if (!odgovaraTipu(sala.getTipSale(), request.getTip())) {
                continue;
            }
            if (sala.getKapacitet() < request.getKapacitet()) {
                continue;
            }

            boolean dostupna = !imaKonfliktTermina(
                    request.getLokacijaId(),
                    sala.getId().getNazivSale(),
                    request.getDatum(),
                    request.getVremePocetka(),
                    request.getVremeZavrsetka(),
                    request.getSesijaId()
            );

            BigDecimal predlozena = dinamickoCeneService
                    .predlogCeneSale(sala.getId().getLokacijaId(), sala.getId().getNazivSale(), request.getDatum())
                    .getPredlozenaCenaPoDanu();

            int skor = izracunajSkor(sala, request, dostupna);

            predlozi.add(PredlogSalaDto.builder()
                    .lokacijaId(sala.getId().getLokacijaId())
                    .nazivSale(sala.getId().getNazivSale())
                    .tipSale(sala.getTipSale())
                    .kapacitet(sala.getKapacitet())
                    .baznaCenaPoDanu(sala.getBaznaCenaPoDanu())
                    .predlozenaCenaPoDanu(predlozena)
                    .skor(skor)
                    .dostupna(dostupna)
                    .obrazlozenje(obrazlozenje(sala, request, dostupna))
                    .build());
        }

        predlozi.sort(Comparator.comparing(PredlogSalaDto::getSkor).reversed());

        List<String> upozorenjaOpreme = proveriKonfliktOpreme(dogadjaj.getDogadjajId(), request);

        return PredlogSalaResponseDto.builder()
                .dogadjajId(request.getDogadjajId())
                .sesijaId(request.getSesijaId())
                .predlozi(predlozi)
                .upozorenjaOpreme(upozorenjaOpreme)
                .build();
    }

    private boolean odgovaraTipu(TipSale tipSale, TipSesije tipSesije) {
        return switch (tipSesije) {
            case KEYNOTE -> tipSale == TipSale.GLAVNA || tipSale == TipSale.FOAJE;
            case WORKSHOP -> tipSale == TipSale.WORKSHOP || tipSale == TipSale.GLAVNA;
            case PANEL -> tipSale == TipSale.PANEL || tipSale == TipSale.GLAVNA;
            case NETWORKING -> tipSale == TipSale.FOAJE || tipSale == TipSale.GLAVNA;
        };
    }

    private boolean imaKonfliktTermina(
            Long lokacijaId, String nazivSale, LocalDate datum,
            LocalTime od, LocalTime doVreme, Long excludeSesijaId
    ) {
        return !sesijaRepository.findPreklapajuce(lokacijaId, nazivSale, datum, od, doVreme, excludeSesijaId).isEmpty();
    }

    private int izracunajSkor(Sala sala, PredlogSalaRequestDto request, boolean dostupna) {
        int skor = dostupna ? 50 : 0;
        int razlikaKap = sala.getKapacitet() - request.getKapacitet();
        if (razlikaKap >= 0 && razlikaKap <= 50) {
            skor += 30;
        } else if (razlikaKap > 50) {
            skor += 10;
        }
        if (sala.getTipSale() == mapTipSesijeNaSalu(request.getTip())) {
            skor += 20;
        }
        return skor;
    }

    private TipSale mapTipSesijeNaSalu(TipSesije tip) {
        return switch (tip) {
            case KEYNOTE -> TipSale.GLAVNA;
            case WORKSHOP -> TipSale.WORKSHOP;
            case PANEL -> TipSale.PANEL;
            case NETWORKING -> TipSale.FOAJE;
        };
    }

    private String obrazlozenje(Sala sala, PredlogSalaRequestDto request, boolean dostupna) {
        if (!dostupna) {
            return "Sala je zauzeta u zadatom terminu.";
        }
        return String.format(
                "Kapacitet %d (traženo %d), tip %s, odgovara sesiji %s.",
                sala.getKapacitet(), request.getKapacitet(), sala.getTipSale(), request.getTip()
        );
    }

    private List<String> proveriKonfliktOpreme(Long dogadjajId, PredlogSalaRequestDto request) {
        List<DetektovanaPotrebaDto> potrebe = potrebaOpremeDetekcijaService.detektujPotrebe(dogadjajId);
        if (potrebe.isEmpty()) {
            return List.of();
        }

        List<Sesija> paralelne = sesijaRepository.findBySala_Lokacija_LokacijaIdAndDatumBetween(
                        request.getLokacijaId(), request.getDatum(), request.getDatum())
                .stream()
                .filter(s -> request.getSesijaId() == null || !s.getSesijaId().equals(request.getSesijaId()))
                .filter(s -> preklapaSe(s, request.getVremePocetka(), request.getVremeZavrsetka()))
                .toList();

        if (paralelne.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> potrebnaKolicina = potrebe.stream()
                .collect(Collectors.toMap(
                        DetektovanaPotrebaDto::getNazivResursa,
                        DetektovanaPotrebaDto::getKolicina,
                        Integer::sum
                ));

        List<String> upozorenja = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : potrebnaKolicina.entrySet()) {
            int paralelnihSesija = paralelne.size() + 1;
            int ukupnaPotreba = entry.getValue() * paralelnihSesija;
            int dostupno = opremaService.dostupnaKolicinaZaNaziv(entry.getKey());
            if (dostupno < ukupnaPotreba) {
                upozorenja.add(String.format(
                        "Paralelne sesije zahtevaju %s ×%d, dostupno na stanju: %d",
                        entry.getKey(), ukupnaPotreba, dostupno));
            }
        }
        return upozorenja;
    }

    private boolean preklapaSe(Sesija s, LocalTime od, LocalTime doVreme) {
        return s.getVremePocetka().isBefore(doVreme) && od.isBefore(s.getVremeZavrsetka());
    }
}
