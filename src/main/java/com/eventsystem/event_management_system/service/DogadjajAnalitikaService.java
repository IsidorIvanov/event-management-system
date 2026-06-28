package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DogadjajAnalitikaDto;
import com.eventsystem.event_management_system.dto.DogadjajAnalitikaDto.TackaDto;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Registracija;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.model.TipKarte;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import com.eventsystem.event_management_system.service.izvestaj.DogadjajIzvestajPdfGenerator;
import com.eventsystem.event_management_system.utils.enums.StatusRegistracije;
import com.eventsystem.event_management_system.utils.enums.VrstaKarte;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class DogadjajAnalitikaService {

    private final DogadjajRepository dogadjajRepository;
    private final SesijaRepository sesijaRepository;
    private final RegistracijaRepository registracijaRepository;
    private final DogadjajIzvestajPdfGenerator pdfGenerator;

    private static final DateTimeFormatter DATUM_FMT = DateTimeFormatter.ofPattern("dd.MM");

    @Transactional(readOnly = true)
    public DogadjajAnalitikaDto getAnalitika(Long dogadjajId) {
        Dogadjaj dogadjaj = dogadjajRepository.findById(dogadjajId)
                .orElseThrow(() -> new NotFoundException("Događaj nije pronađen sa id: " + dogadjajId));

        int maksKapacitet = dogadjaj.getMaksKapacitet() != null ? dogadjaj.getMaksKapacitet() : 0;

        List<Registracija> potvrdjene = registracijaRepository.findByDogadjajIdWithDetails(dogadjajId).stream()
                .filter(r -> r.getStatus() == StatusRegistracije.POTVRDJENA)
                .toList();

        long ukupnoRegistracija = potvrdjene.size();

        List<TackaDto> stopaPrisustvaSerija = buildStopaPrisustvaSerija(potvrdjene, maksKapacitet);
        List<TackaDto> engagementPoSesiji = buildEngagementPoSesiji(dogadjajId, potvrdjene);

        double stopaPrisustva = maksKapacitet > 0
                ? round1((double) ukupnoRegistracija / maksKapacitet * 100.0)
                : 0.0;
        double prosecniEngagement = engagementPoSesiji.isEmpty()
                ? 0.0
                : round1(engagementPoSesiji.stream().mapToDouble(TackaDto::getVrednost).average().orElse(0.0));

        return DogadjajAnalitikaDto.builder()
                .dogadjajId(dogadjaj.getDogadjajId())
                .dogadjajNaziv(dogadjaj.getNaziv())
                .maksKapacitet(maksKapacitet)
                .ukupnoRegistracija(ukupnoRegistracija)
                .stopaPrisustva(stopaPrisustva)
                .prosecniEngagement(prosecniEngagement)
                .stopaPrisustvaSerija(stopaPrisustvaSerija)
                .engagementPoSesiji(engagementPoSesiji)
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadPdf(Long dogadjajId) {
        DogadjajAnalitikaDto analitika = getAnalitika(dogadjajId);
        byte[] pdf = pdfGenerator.generate(analitika);
        String filename = "izvestaj-dogadjaj-" + dogadjajId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(pdf));
    }

    /** Kumulativne potvrđene registracije po datumu registracije, izražene kao % maks. kapaciteta. */
    private List<TackaDto> buildStopaPrisustvaSerija(List<Registracija> potvrdjene, int maksKapacitet) {
        TreeMap<LocalDate, Long> poDatumu = new TreeMap<>();
        for (Registracija r : potvrdjene) {
            LocalDate datum = r.getDatumRegistracije();
            if (datum == null) continue;
            poDatumu.merge(datum, 1L, Long::sum);
        }

        List<TackaDto> serija = new ArrayList<>();
        long kumulativno = 0;
        for (var unos : poDatumu.entrySet()) {
            kumulativno += unos.getValue();
            double vrednost = maksKapacitet > 0
                    ? round1((double) kumulativno / maksKapacitet * 100.0)
                    : 0.0;
            serija.add(new TackaDto(unos.getKey().format(DATUM_FMT), vrednost));
        }
        return serija;
    }

    /** Popunjenost (%) po sesiji = potvrđene registracije koje pokrivaju sesiju / kapacitet sesije. */
    private List<TackaDto> buildEngagementPoSesiji(Long dogadjajId, List<Registracija> potvrdjene) {
        List<Sesija> sesije = sesijaRepository.findAllByDogadjaj_DogadjajId(dogadjajId);
        sesije.sort(Comparator.comparing(Sesija::getDatum).thenComparing(Sesija::getVremePocetka));

        List<TackaDto> rezultat = new ArrayList<>();
        for (Sesija s : sesije) {
            long popunjenost = potvrdjene.stream().filter(r -> pokrivaSesiju(r, s)).count();
            int kapacitet = s.getKapacitet() != null ? s.getKapacitet() : 0;
            double engagement = kapacitet > 0
                    ? round1((double) popunjenost / kapacitet * 100.0)
                    : 0.0;
            rezultat.add(new TackaDto(s.getNaziv(), engagement));
        }
        return rezultat;
    }

    /** Da li potvrđena registracija (po vrsti karte) pokriva datu sesiju. Vidi SesijaService. */
    private boolean pokrivaSesiju(Registracija r, Sesija s) {
        TipKarte tk = r.getTipKarte();
        VrstaKarte vrsta = tk.getVrsta();
        String nazivTipa = tk.getId().getNazivTipa();
        return switch (vrsta) {
            case VISEDNEVNA, BESPLATNA -> true;
            case JEDNODNEVNA -> {
                try {
                    yield s.getDatum().equals(LocalDate.parse(nazivTipa));
                } catch (Exception e) {
                    yield false;
                }
            }
            case POJEDINACNA_SESIJA -> s.getNaziv().equalsIgnoreCase(nazivTipa);
        };
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
