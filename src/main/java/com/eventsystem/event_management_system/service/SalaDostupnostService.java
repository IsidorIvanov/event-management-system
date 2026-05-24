package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.SalaDostupnostDto;
import com.eventsystem.event_management_system.dto.SalaDostupnostResponseDto;
import com.eventsystem.event_management_system.model.Sala;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import com.eventsystem.event_management_system.repository.SalaRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaDostupnostService {

    private static final int SAT_POCETKA = 8;
    private static final int SAT_ZAVRSETKA = 20;
    private static final int MAX_DANA = 31;

    private final LokacijaRepository lokacijaRepository;
    private final SalaRepository salaRepository;
    private final SesijaRepository sesijaRepository;

    @Transactional(readOnly = true)
    public SalaDostupnostResponseDto getDostupnost(Long lokacijaId, LocalDate datumOd, LocalDate datumDo) {
        if (!lokacijaRepository.existsById(lokacijaId)) {
            throw new RuntimeException("Lokacija nije pronadjena sa id: " + lokacijaId);
        }
        validateDateRange(datumOd, datumDo);

        List<Sala> sale = salaRepository.findByLokacija_LokacijaId(lokacijaId);
        List<Sesija> sesije = sesijaRepository.findBySala_Lokacija_LokacijaIdAndDatumBetween(lokacijaId, datumOd, datumDo);

        Map<String, Map<LocalDate, List<Sesija>>> sesijePoSaliIDanu = sesije.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getSala().getId().getNazivSale(),
                        Collectors.groupingBy(Sesija::getDatum)
                ));

        List<SalaDostupnostDto> saleDostupnost = sale.stream()
                .map(sala -> buildSalaDostupnost(sala, datumOd, datumDo, sesijePoSaliIDanu))
                .toList();

        return SalaDostupnostResponseDto.builder()
                .lokacijaId(lokacijaId)
                .datumOd(datumOd)
                .datumDo(datumDo)
                .satPocetka(SAT_POCETKA)
                .satZavrsetka(SAT_ZAVRSETKA)
                .sale(saleDostupnost)
                .build();
    }

    private SalaDostupnostDto buildSalaDostupnost(
            Sala sala,
            LocalDate datumOd,
            LocalDate datumDo,
            Map<String, Map<LocalDate, List<Sesija>>> sesijePoSaliIDanu) {

        String nazivSale = sala.getId().getNazivSale();
        Map<LocalDate, List<Sesija>> sesijePoDanu = sesijePoSaliIDanu.getOrDefault(nazivSale, Map.of());

        List<SalaDostupnostDto.DanDostupnostiDto> dani = new ArrayList<>();
        for (LocalDate datum = datumOd; !datum.isAfter(datumDo); datum = datum.plusDays(1)) {
            List<Sesija> sesijeNaDan = sesijePoDanu.getOrDefault(datum, List.of());
            dani.add(SalaDostupnostDto.DanDostupnostiDto.builder()
                    .datum(datum)
                    .slotovi(buildSlotovi(sesijeNaDan))
                    .build());
        }

        return SalaDostupnostDto.builder()
                .lokacijaId(sala.getId().getLokacijaId())
                .nazivSale(nazivSale)
                .datumOd(datumOd)
                .datumDo(datumDo)
                .satPocetka(SAT_POCETKA)
                .satZavrsetka(SAT_ZAVRSETKA)
                .dani(dani)
                .build();
    }

    private List<SalaDostupnostDto.VremenskiSlotDto> buildSlotovi(List<Sesija> sesijeNaDan) {
        List<SalaDostupnostDto.VremenskiSlotDto> slotovi = new ArrayList<>();
        for (int sat = SAT_POCETKA; sat < SAT_ZAVRSETKA; sat++) {
            LocalTime slotOd = LocalTime.of(sat, 0);
            LocalTime slotDo = LocalTime.of(sat + 1, 0);
            Sesija preklapanje = findOverlappingSession(sesijeNaDan, slotOd, slotDo);

            slotovi.add(SalaDostupnostDto.VremenskiSlotDto.builder()
                    .vremeOd(slotOd)
                    .vremeDo(slotDo)
                    .dostupno(preklapanje == null)
                    .nazivSesije(preklapanje != null ? preklapanje.getNaziv() : null)
                    .sesijaId(preklapanje != null ? preklapanje.getSesijaId() : null)
                    .build());
        }
        return slotovi;
    }

    private Sesija findOverlappingSession(List<Sesija> sesije, LocalTime slotOd, LocalTime slotDo) {
        for (Sesija sesija : sesije) {
            if (sesija.getVremePocetka().isBefore(slotDo) && sesija.getVremeZavrsetka().isAfter(slotOd)) {
                return sesija;
            }
        }
        return null;
    }

    private void validateDateRange(LocalDate datumOd, LocalDate datumDo) {
        if (datumOd == null || datumDo == null) {
            throw new RuntimeException("Datum od i datum do su obavezni.");
        }
        if (datumDo.isBefore(datumOd)) {
            throw new RuntimeException("Datum do mora biti posle ili jednak datumu od.");
        }
        if (datumOd.plusDays(MAX_DANA).isBefore(datumDo)) {
            throw new RuntimeException("Maksimalan opseg je " + MAX_DANA + " dana.");
        }
    }
}
