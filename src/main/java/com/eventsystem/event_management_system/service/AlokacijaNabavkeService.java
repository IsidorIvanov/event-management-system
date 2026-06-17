package com.eventsystem.event_management_system.service;



import com.eventsystem.event_management_system.dto.*;

import com.eventsystem.event_management_system.model.Cenovnik;

import com.eventsystem.event_management_system.model.Nabavka;

import com.eventsystem.event_management_system.model.StavkaNabavke;

import com.eventsystem.event_management_system.repository.NabavkaRepository;

import com.eventsystem.event_management_system.repository.CenovnikRepository;

import com.eventsystem.event_management_system.utils.TekstNormalizacija;

import com.eventsystem.event_management_system.utils.enums.StatusNabavke;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.math.BigDecimal;

import java.math.RoundingMode;

import java.util.*;

import java.util.stream.Collectors;



/**

 * Greedy optimizacija: za svaku stavku bira najjeftinijeg dostupnog dobavljača iz cenovnika.

 */

@Service

@RequiredArgsConstructor

public class AlokacijaNabavkeService {



    private static final int MONEY_SCALE = 2;



    private final CenovnikRepository cenovnikRepository;

    private final NabavkaService nabavkaService;

    private final NabavkaRepository nabavkaRepository;

    private final DobavljacService dobavljacService;



    @Transactional(readOnly = true)

    public PlanAlokacijeDto optimizuj(List<PotrebnaStavkaDto> potrebne) {

        List<Cenovnik> ponude = cenovnikRepository.findSveDostupne();

        List<StavkaAlokacijeDto> stavke = new ArrayList<>();

        List<String> upozorenja = new ArrayList<>();

        BigDecimal ukupno = BigDecimal.ZERO;

        Set<Long> dobavljaci = new HashSet<>();

        int pokriveno = 0;



        for (PotrebnaStavkaDto potrebna : potrebne) {

            Optional<Cenovnik> najbolja = ponude.stream()

                    .filter(c -> TekstNormalizacija.naziviSePodudaraju(c.getNazivResursa(), potrebna.getNazivResursa()))

                    .min(Comparator.comparing(Cenovnik::getCenaJedinicna));



            if (najbolja.isEmpty()) {

                stavke.add(StavkaAlokacijeDto.builder()

                        .nazivResursa(potrebna.getNazivResursa())

                        .kolicina(potrebna.getKolicina())

                        .pokriveno(false)

                        .build());

                upozorenja.add("Nema ponude u cenovniku za: " + potrebna.getNazivResursa());

                continue;

            }



            Cenovnik c = najbolja.get();

            BigDecimal jedinicna = c.getCenaJedinicna();

            BigDecimal stavkaUkupno = jedinicna.multiply(BigDecimal.valueOf(potrebna.getKolicina()))

                    .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);

            Long dobavljacId = c.getDobavljac().getDobavljacId();

            dobavljaci.add(dobavljacId);

            ukupno = ukupno.add(stavkaUkupno);

            pokriveno++;



            stavke.add(StavkaAlokacijeDto.builder()

                    .nazivResursa(potrebna.getNazivResursa())

                    .kolicina(potrebna.getKolicina())

                    .cenovnikId(c.getCenovnikId())

                    .dobavljacId(dobavljacId)

                    .dobavljacNaziv(c.getDobavljac().getNaziv())

                    .jedinicnaCena(jedinicna)

                    .ukupnaCena(stavkaUkupno)

                    .pokriveno(true)

                    .build());

        }



        if (dobavljaci.size() > 1) {

            upozorenja.add("Plan koristi " + dobavljaci.size()

                    + " dobavljača — pri generisanju porudžbenica kreira se po jedna po dobavljaču.");

        }



        return PlanAlokacijeDto.builder()

                .stavke(stavke)

                .ukupnaCena(ukupno.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN))

                .brojDobavljaca(dobavljaci.size())

                .svePokriveno(pokriveno == potrebne.size())

                .upozorenja(upozorenja)

                .build();

    }



    @Transactional

    public PlanAlokacijeDto primeni(Long nabavkaId, List<PotrebnaStavkaDto> potrebne) {

        PlanAlokacijeDto plan = optimizuj(potrebne);

        if (!plan.isSvePokriveno()) {

            throw new RuntimeException("Nije moguće primeniti plan — neke stavke nemaju ponudu u cenovniku.");

        }



        Nabavka nabavka = nabavkaService.findEntityWithDetalji(nabavkaId);

        List<StavkaNabavkeDto> stavkeDto = plan.getStavke().stream()

                .map(s -> StavkaNabavkeDto.builder()

                        .nazivResursa(s.getNazivResursa())

                        .kolicina(s.getKolicina())

                        .cenovnikId(s.getCenovnikId())

                        .jedinicnaCena(s.getJedinicnaCena())

                        .build())

                .toList();



        nabavka.getStavke().clear();

        nabavkaService.applyPredlogStavki(nabavka, stavkeDto);

        dodeliDominantnogDobavljaca(nabavka);

        nabavka.setStatus(StatusNabavke.U_OBRADI);

        nabavkaRepository.saveAndFlush(nabavka);

        return plan;

    }



    private void dodeliDominantnogDobavljaca(Nabavka nabavka) {

        Map<Long, BigDecimal> vrednost = nabavka.getStavke().stream()

                .filter(s -> s.getCenovnik() != null && s.getCenovnik().getDobavljac() != null)

                .collect(Collectors.groupingBy(

                        s -> s.getCenovnik().getDobavljac().getDobavljacId(),

                        Collectors.reducing(BigDecimal.ZERO, StavkaNabavke::getUkupnaCena, BigDecimal::add)

                ));



        if (vrednost.isEmpty()) {

            return;

        }



        Long dominantni = vrednost.entrySet().stream()

                .max(Map.Entry.comparingByValue())

                .map(Map.Entry::getKey)

                .orElseThrow();



        dobavljacService.assertAvailableForNabavka(dominantni);

        nabavka.setDobavljac(dobavljacService.findEntity(dominantni));

    }

}

