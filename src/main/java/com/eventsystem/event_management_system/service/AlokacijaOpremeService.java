package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.*;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.DodelaOpreme;
import com.eventsystem.event_management_system.model.Oprema;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.repository.DodelaOpremeRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.OpremaRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import com.eventsystem.event_management_system.utils.TekstNormalizacija;
import com.eventsystem.event_management_system.utils.enums.StatusDodeleOpreme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlokacijaOpremeService {

    private static final List<StatusDodeleOpreme> AKTIVNE_DODELE = List.of(
            StatusDodeleOpreme.REZERVISANO,
            StatusDodeleOpreme.NA_DOGADJAJU
    );

    private final PotrebaOpremeDetekcijaService potrebaOpremeDetekcijaService;
    private final DogadjajRepository dogadjajRepository;
    private final SesijaRepository sesijaRepository;
    private final OpremaRepository opremaRepository;
    private final DodelaOpremeRepository dodelaOpremeRepository;
    private final DodelaOpremeService dodelaOpremeService;

    @Transactional(readOnly = true)
    public PlanAlokacijeOpremeDto predlogZaDogadjaj(Long dogadjajId) {
        Dogadjaj dogadjaj = dogadjajRepository.findById(dogadjajId)
                .orElseThrow(() -> new NotFoundException("Događaj nije pronađen sa id: " + dogadjajId));

        List<DetektovanaPotrebaDto> potrebe = potrebaOpremeDetekcijaService.detektujPotrebe(dogadjajId);
        List<Sesija> sesije = sesijaRepository.findAllByDogadjaj_DogadjajId(dogadjajId);
        List<DodelaOpreme> postojeceDodele = dodelaOpremeRepository.findByDogadjajWithDetalji(dogadjajId).stream()
                .filter(d -> AKTIVNE_DODELE.contains(d.getStatus()))
                .toList();

        Map<Long, Integer> preostaloNaStanju = opremaRepository.findAll().stream()
                .collect(Collectors.toMap(Oprema::getOpremaId, Oprema::getKolicinaDostupno, Integer::sum));

        LocalDate datumOd = dogadjaj.getDatumPocetka();
        LocalDate datumDo = dogadjaj.getDatumZavrsetka();
        Sesija referentnaSesija = sesije.isEmpty() ? null : sesije.getFirst();

        List<StavkaAlokacijeOpremeDto> stavke = new ArrayList<>();
        List<String> upozorenja = new ArrayList<>();
        int pokrivenePotrebe = 0;

        if (potrebe.isEmpty()) {
            upozorenja.add("Nema detektovanih potreba za opremu — proverite cenovnik i parametre događaja.");
        }

        for (DetektovanaPotrebaDto potreba : potrebe) {
            int vecDodeljeno = sumVecDodeljeno(postojeceDodele, potreba.getNazivResursa());
            int josPotrebno = Math.max(0, potreba.getKolicina() - vecDodeljeno);

            if (josPotrebno == 0) {
                stavke.add(stavkaVecPokriveno(potreba, vecDodeljeno, datumOd, datumDo, referentnaSesija));
                pokrivenePotrebe++;
                continue;
            }

            List<Oprema> kandidati = opremaRepository.findAll().stream()
                    .filter(o -> TekstNormalizacija.naziviSePodudaraju(o.getNaziv(), potreba.getNazivResursa()))
                    .filter(o -> preostaloNaStanju.getOrDefault(o.getOpremaId(), 0) > 0)
                    .sorted(Comparator.comparingInt((Oprema o) -> preostaloNaStanju.getOrDefault(o.getOpremaId(), 0)).reversed())
                    .toList();

            int preostaloZaPotrebu = josPotrebno;
            boolean dodeljeno = false;

            for (Oprema oprema : kandidati) {
                if (preostaloZaPotrebu <= 0) {
                    break;
                }
                int dostupno = preostaloNaStanju.getOrDefault(oprema.getOpremaId(), 0);
                if (dostupno <= 0) {
                    continue;
                }
                int uzmi = Math.min(preostaloZaPotrebu, dostupno);
                preostaloNaStanju.put(oprema.getOpremaId(), dostupno - uzmi);
                preostaloZaPotrebu -= uzmi;
                dodeljeno = true;

                stavke.add(StavkaAlokacijeOpremeDto.builder()
                        .nazivPotrebe(potreba.getNazivResursa())
                        .potrebnaKolicina(potreba.getKolicina())
                        .vecDodeljeno(vecDodeljeno)
                        .predlozenaKolicina(uzmi)
                        .opremaId(oprema.getOpremaId())
                        .opremaNaziv(oprema.getNaziv())
                        .kategorija(oprema.getKategorija())
                        .sesijaId(referentnaSesija != null ? referentnaSesija.getSesijaId() : null)
                        .sesijaNaziv(referentnaSesija != null ? referentnaSesija.getNaziv() : null)
                        .datumOd(datumOd)
                        .datumDo(datumDo)
                        .pokriveno(true)
                        .obrazlozenje(String.format(
                                "Dodela %d kom iz inventara (%s) za period %s – %s",
                                uzmi, oprema.getNaziv(), datumOd, datumDo))
                        .build());
            }

            if (preostaloZaPotrebu > 0) {
                upozorenja.add(String.format(
                        "Nedovoljno opreme za „%s“: još %d kom (već dodeljeno %d od %d)",
                        potreba.getNazivResursa(), preostaloZaPotrebu, vecDodeljeno, potreba.getKolicina()));
                stavke.add(StavkaAlokacijeOpremeDto.builder()
                        .nazivPotrebe(potreba.getNazivResursa())
                        .potrebnaKolicina(potreba.getKolicina())
                        .vecDodeljeno(vecDodeljeno)
                        .predlozenaKolicina(0)
                        .datumOd(datumOd)
                        .datumDo(datumDo)
                        .pokriveno(false)
                        .obrazlozenje(dodeljeno
                                ? "Delimično pokriveno — nedostaje još " + preostaloZaPotrebu + " kom"
                                : "Nema dostupne opreme na stanju")
                        .build());
            } else if (dodeljeno) {
                pokrivenePotrebe++;
            }
        }

        int ukupnoPotreba = potrebe.size();
        boolean sve = ukupnoPotreba > 0 && pokrivenePotrebe == ukupnoPotreba;

        return PlanAlokacijeOpremeDto.builder()
                .dogadjajId(dogadjajId)
                .dogadjajNaziv(dogadjaj.getNaziv())
                .svePokriveno(sve)
                .ukupnoPotreba(ukupnoPotreba)
                .pokrivenoStavki(pokrivenePotrebe)
                .stavke(stavke)
                .upozorenja(upozorenja)
                .build();
    }

    @Transactional
    public PrimeniAlokacijuOpremeResponseDto primeni(Long dogadjajId) {
        PlanAlokacijeOpremeDto plan = predlogZaDogadjaj(dogadjajId);
        List<DodelaOpremeDto> kreirane = new ArrayList<>();

        for (StavkaAlokacijeOpremeDto stavka : plan.getStavke()) {
            if (!stavka.isPokriveno() || stavka.getPredlozenaKolicina() <= 0 || stavka.getOpremaId() == null) {
                continue;
            }
            DodelaOpremeDto dto = DodelaOpremeDto.builder()
                    .opremaId(stavka.getOpremaId())
                    .dogadjajId(dogadjajId)
                    .sesijaId(stavka.getSesijaId())
                    .kolicina(stavka.getPredlozenaKolicina())
                    .datumOd(stavka.getDatumOd())
                    .datumDo(stavka.getDatumDo())
                    .napomena("Automatska alokacija: " + stavka.getNazivPotrebe())
                    .build();
            kreirane.add(dodelaOpremeService.rezervisi(dto));
        }

        if (kreirane.isEmpty()) {
            throw new RuntimeException("Nema stavki za primenu — sve potrebe su već pokrivene ili nema zaliha u inventaru.");
        }

        return PrimeniAlokacijuOpremeResponseDto.builder()
                .plan(plan)
                .kreiraneDodele(kreirane)
                .build();
    }

    private StavkaAlokacijeOpremeDto stavkaVecPokriveno(
            DetektovanaPotrebaDto potreba,
            int vecDodeljeno,
            LocalDate datumOd,
            LocalDate datumDo,
            Sesija sesija
    ) {
        return StavkaAlokacijeOpremeDto.builder()
                .nazivPotrebe(potreba.getNazivResursa())
                .potrebnaKolicina(potreba.getKolicina())
                .vecDodeljeno(vecDodeljeno)
                .predlozenaKolicina(0)
                .sesijaId(sesija != null ? sesija.getSesijaId() : null)
                .sesijaNaziv(sesija != null ? sesija.getNaziv() : null)
                .datumOd(datumOd)
                .datumDo(datumDo)
                .pokriveno(true)
                .obrazlozenje("Potreba je već pokrivena postojećim dodelama (" + vecDodeljeno + " kom)")
                .build();
    }

    private int sumVecDodeljeno(List<DodelaOpreme> dodele, String nazivPotrebe) {
        return dodele.stream()
                .filter(d -> TekstNormalizacija.naziviSePodudaraju(d.getOprema().getNaziv(), nazivPotrebe))
                .mapToInt(DodelaOpreme::getKolicina)
                .sum();
    }
}
