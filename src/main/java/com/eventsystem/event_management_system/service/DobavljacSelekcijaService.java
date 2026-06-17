package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.*;
import com.eventsystem.event_management_system.model.Cenovnik;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.model.Nabavka;
import com.eventsystem.event_management_system.repository.CenovnikRepository;
import com.eventsystem.event_management_system.utils.TekstNormalizacija;
import com.eventsystem.event_management_system.utils.enums.KriterijumSelekcijeDobavljaca;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DobavljacSelekcijaService {

    private final CenovnikRepository cenovnikRepository;
    private final NabavkaService nabavkaService;
    private final DobavljacService dobavljacService;

    @Transactional(readOnly = true)
    public PredlogDobavljacaDto predloziDobavljaca(AutomatskaSelekcijaRequestDto request) {
        return izracunajPredlog(request.getKriterijum(), request.getPotrebneStavke());
    }

    @Transactional(readOnly = true)
    public SelekcijaDobavljacaResponseDto predloziSaAlternativama(AutomatskaSelekcijaRequestDto request) {
        List<PredlogDobavljacaDto> svi = izracunajSvePredloge(request.getKriterijum(), request.getPotrebneStavke());
        if (svi.isEmpty()) {
            throw new RuntimeException("Nema dostupnih ponuda u cenovniku za trazene stavke.");
        }
        return SelekcijaDobavljacaResponseDto.builder()
                .predlog(svi.getFirst())
                .alternative(svi.size() > 1 ? svi.subList(1, svi.size()) : List.of())
                .build();
    }

    @Transactional
    public PredlogDobavljacaDto primeniAutomatskuSelekciju(AutomatskaSelekcijaRequestDto request) {
        if (request.getNabavkaId() == null) {
            throw new RuntimeException("ID nabavke je obavezan za primenu selekcije.");
        }
        PredlogDobavljacaDto predlog = izracunajPredlog(request.getKriterijum(), request.getPotrebneStavke());
        Nabavka nabavka = nabavkaService.findEntityWithDetalji(request.getNabavkaId());

        nabavka.setSelekcijaKriterijum(request.getKriterijum());
        nabavka.setDobavljac(dobavljacService.findEntity(predlog.getDobavljacId()));

        List<StavkaNabavkeDto> stavke = predlog.getMapiranjeStavki().stream()
                .map(s -> StavkaNabavkeDto.builder()
                        .nazivResursa(s.getNazivResursa())
                        .kolicina(s.getKolicina())
                        .jedinicnaCena(s.getJedinicnaCena())
                        .cenovnikId(s.getCenovnikId())
                        .build())
                .toList();

        nabavka.getStavke().clear();
        nabavkaService.applyPredlogStavki(nabavka, stavke);
        nabavkaService.updateStatus(request.getNabavkaId(), StatusNabavke.U_OBRADI);

        return predlog;
    }

    private PredlogDobavljacaDto izracunajPredlog(
            KriterijumSelekcijeDobavljaca kriterijum,
            List<PotrebnaStavkaDto> potrebne
    ) {
        Map<Long, SupplierScore> scores = buildScores(potrebne);

        if (scores.isEmpty()) {
            throw new RuntimeException("Nema dostupnih ponuda u cenovniku za trazene stavke.");
        }

        int ukupnoStavki = potrebne.size();
        Optional<SupplierScore> najboljiPuni = scores.values().stream()
                .filter(s -> s.pokriveneStavke == ukupnoStavki)
                .min(comparatorZa(kriterijum));

        boolean pokrivaSve = najboljiPuni.isPresent();
        SupplierScore izabrani = najboljiPuni.orElseGet(() ->
                scores.values().stream().min(comparatorZa(kriterijum)).orElseThrow());

        return izabrani.toPredlog(kriterijum, pokrivaSve);
    }

    private List<PredlogDobavljacaDto> izracunajSvePredloge(
            KriterijumSelekcijeDobavljaca kriterijum,
            List<PotrebnaStavkaDto> potrebne
    ) {
        Map<Long, SupplierScore> scores = buildScores(potrebne);
        int ukupnoStavki = potrebne.size();
        return scores.values().stream()
                .sorted(comparatorZa(kriterijum))
                .map(s -> s.toPredlog(kriterijum, s.pokriveneStavke == ukupnoStavki))
                .toList();
    }

    private Map<Long, SupplierScore> buildScores(List<PotrebnaStavkaDto> potrebne) {
        Map<Long, SupplierScore> scores = new HashMap<>();
        List<Cenovnik> svePonude = cenovnikRepository.findSveDostupne();

        for (PotrebnaStavkaDto potrebna : potrebne) {
            List<Cenovnik> ponude = svePonude.stream()
                    .filter(c -> TekstNormalizacija.naziviSePodudaraju(c.getNazivResursa(), potrebna.getNazivResursa()))
                    .toList();
            for (Cenovnik cenovnik : ponude) {
                Long dobavljacId = cenovnik.getDobavljac().getDobavljacId();
                SupplierScore score = scores.computeIfAbsent(dobavljacId, id -> new SupplierScore(cenovnik.getDobavljac()));
                BigDecimal ukupno = cenovnik.getCenaJedinicna().multiply(BigDecimal.valueOf(potrebna.getKolicina()));
                score.dodajStavku(potrebna.getNazivResursa(), potrebna.getKolicina(), cenovnik, ukupno);
            }
        }
        return scores;
    }

    private Comparator<SupplierScore> comparatorZa(KriterijumSelekcijeDobavljaca kriterijum) {
        Comparator<BigDecimal> rejtingDesc = Comparator.nullsLast(Comparator.<BigDecimal>reverseOrder());
        if (kriterijum == KriterijumSelekcijeDobavljaca.NAJBOLJI_REJTING) {
            return Comparator.comparing((SupplierScore s) -> s.dobavljac.getRejting(), rejtingDesc)
                    .thenComparing(s -> s.ukupnaCena);
        }
        return Comparator.comparing((SupplierScore s) -> s.ukupnaCena)
                .thenComparing((SupplierScore s) -> s.dobavljac.getRejting(), rejtingDesc);
    }

    private static class SupplierScore {
        private final Dobavljac dobavljac;
        private BigDecimal ukupnaCena = BigDecimal.ZERO;
        private int pokriveneStavke = 0;
        private final Set<String> pokriveniNazivi = new HashSet<>();
        private final List<PredlogDobavljacaDto.StavkaPredlogDto> mapiranje = new ArrayList<>();

        SupplierScore(Dobavljac dobavljac) {
            this.dobavljac = dobavljac;
        }

        void dodajStavku(String naziv, int kolicina, Cenovnik cenovnik, BigDecimal ukupno) {
            if (pokriveniNazivi.add(naziv.toLowerCase())) {
                pokriveneStavke++;
            }
            ukupnaCena = ukupnaCena.add(ukupno);
            mapiranje.add(PredlogDobavljacaDto.StavkaPredlogDto.builder()
                    .nazivResursa(naziv)
                    .kolicina(kolicina)
                    .cenovnikId(cenovnik.getCenovnikId())
                    .jedinicnaCena(cenovnik.getCenaJedinicna())
                    .ukupnaCena(ukupno)
                    .build());
        }

        PredlogDobavljacaDto toPredlog(KriterijumSelekcijeDobavljaca kriterijum, boolean pokrivaSve) {
            return PredlogDobavljacaDto.builder()
                    .dobavljacId(dobavljac.getDobavljacId())
                    .dobavljacNaziv(dobavljac.getNaziv())
                    .kontaktEmail(dobavljac.getKontaktEmail())
                    .rejting(dobavljac.getRejting())
                    .ukupnaProcenjenaCena(ukupnaCena)
                    .kriterijum(kriterijum)
                    .pokrivaSveStavke(pokrivaSve)
                    .mapiranjeStavki(mapiranje)
                    .build();
        }
    }
}
