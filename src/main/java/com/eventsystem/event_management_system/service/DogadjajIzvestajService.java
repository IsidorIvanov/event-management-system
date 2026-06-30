package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DetektovanaPotrebaDto;
import com.eventsystem.event_management_system.dto.DodelaOpremeDto;
import com.eventsystem.event_management_system.dto.DogadjajResursiTroskoviIzvestajDto;
import com.eventsystem.event_management_system.dto.IzvestajPodaciDto;
import com.eventsystem.event_management_system.dto.SesijaDto;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.AnalizaProfitabilnosti;
import com.eventsystem.event_management_system.model.Budzet;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.DodelaOpreme;
import com.eventsystem.event_management_system.model.Nabavka;
import com.eventsystem.event_management_system.model.StavkaBudzeta;
import com.eventsystem.event_management_system.model.StavkaNabavke;
import com.eventsystem.event_management_system.repository.AnalizaProfitabilnostiRepository;
import com.eventsystem.event_management_system.repository.BudzetRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.DodelaOpremeRepository;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import com.eventsystem.event_management_system.repository.NabavkaRepository;
import com.eventsystem.event_management_system.utils.TekstNormalizacija;
import com.eventsystem.event_management_system.utils.enums.AnalizaStatus;
import com.eventsystem.event_management_system.utils.enums.RezultatOcene;
import com.eventsystem.event_management_system.utils.enums.StatusDodeleOpreme;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DogadjajIzvestajService {

    private static final int MONEY_SCALE = 2;
    private static final int RATIO_SCALE = 4;
    private static final List<StatusDodeleOpreme> AKTIVNE_DODELE = List.of(
            StatusDodeleOpreme.REZERVISANO,
            StatusDodeleOpreme.NA_DOGADJAJU
    );
    private static final List<StatusNabavke> COMMITTED_NABAVKA = List.of(
            StatusNabavke.POTVRDJENA,
            StatusNabavke.U_ISPORUCI,
            StatusNabavke.ZAVRSENA
    );

    private final DogadjajRepository dogadjajRepository;
    private final SesijaService sesijaService;
    private final PotrebaOpremeDetekcijaService potrebaOpremeDetekcijaService;
    private final OpremaService opremaService;
    private final DodelaOpremeRepository dodelaOpremeRepository;
    private final DodelaOpremeService dodelaOpremeService;
    private final NabavkaRepository nabavkaRepository;
    private final FinansijskaAgregacijaService finansijskaAgregacijaService;
    private final BudzetRepository budzetRepository;
    private final AnalizaProfitabilnostiRepository analizaProfitabilnostiRepository;
    private final LokacijaRepository lokacijaRepository;

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public DogadjajResursiTroskoviIzvestajDto getKompletanIzvestaj(Long dogadjajId) {
        Dogadjaj dogadjaj = requireDogadjaj(dogadjajId);
        DogadjajResursiTroskoviIzvestajDto.ResursiDto resursi = buildResursi(dogadjajId);
        DogadjajResursiTroskoviIzvestajDto.TroskoviDto troskovi = buildTroskovi(dogadjajId);

        return DogadjajResursiTroskoviIzvestajDto.builder()
                .dogadjajId(dogadjajId)
                .dogadjajNaziv(dogadjaj.getNaziv())
                .generisanoAt(LocalDateTime.now())
                .resursi(resursi)
                .troskovi(troskovi)
                .rezime(buildRezime(resursi, troskovi))
                .build();
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public DogadjajResursiTroskoviIzvestajDto.ResursiDto getResursi(Long dogadjajId) {
        requireDogadjaj(dogadjajId);
        return buildResursi(dogadjajId);
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public DogadjajResursiTroskoviIzvestajDto.TroskoviDto getTroskovi(Long dogadjajId) {
        requireDogadjaj(dogadjajId);
        return buildTroskovi(dogadjajId);
    }

    @Transactional(readOnly = true)
    public IzvestajPodaciDto toIzvestajPodaci(DogadjajResursiTroskoviIzvestajDto izvestaj) {
        DogadjajResursiTroskoviIzvestajDto.TroskoviDto t = izvestaj.getTroskovi();
        DogadjajResursiTroskoviIzvestajDto.ResursiDto r = izvestaj.getResursi();

        List<IzvestajPodaciDto.IzvestajSalaRedDto> sale = r.getStavkeSala().stream()
                .map(s -> IzvestajPodaciDto.IzvestajSalaRedDto.builder()
                        .sesijaNaziv(s.getSesijaNaziv())
                        .nazivSale(s.getNazivSale())
                        .datum(s.getDatum())
                        .kapacitet(s.getKapacitet())
                        .popunjenost(s.getPopunjenost())
                        .iskoriscenostProcenat(s.getIskoriscenostProcenat())
                        .build())
                .toList();

        List<IzvestajPodaciDto.IzvestajOpremeRedDto> oprema = r.getStavkeOpreme().stream()
                .map(o -> IzvestajPodaciDto.IzvestajOpremeRedDto.builder()
                        .nazivResursa(o.getNazivResursa())
                        .potrebnaKolicina(o.getPotrebnaKolicina())
                        .dodeljeno(o.getDodeljenoDogadjaju())
                        .dostupnoNaStanju(o.getDostupnoNaStanju())
                        .pokriveno(o.isPokriveno())
                        .build())
                .toList();

        List<IzvestajPodaciDto.IzvestajNabavkaRedDto> nabavke = r.getStavkeNabavke().stream()
                .map(n -> IzvestajPodaciDto.IzvestajNabavkaRedDto.builder()
                        .nabavkaId(n.getNabavkaId())
                        .status(n.getStatus())
                        .dobavljacNaziv(n.getDobavljacNaziv())
                        .brojStavki(n.getBrojStavki())
                        .ukupnaVrednost(n.getUkupnaVrednost())
                        .commitovana(n.isCommitovana())
                        .build())
                .toList();

        List<IzvestajPodaciDto.IzvestajBudzetRedDto> budzet = t.getIskoriscenostBudzeta().stream()
                .map(b -> IzvestajPodaciDto.IzvestajBudzetRedDto.builder()
                        .nazivBudzeta(b.getNazivBudzeta())
                        .kategorijaNaziv(b.getKategorijaNaziv())
                        .planirano(b.getPlanirano())
                        .stvarno(b.getStvarno())
                        .iskoriscenostProcenat(b.getIskoriscenostProcenat())
                        .statusKontrole(b.getStatusKontrole())
                        .build())
                .toList();

        return IzvestajPodaciDto.builder()
                .naslov("Izveštaj resursa i troškova po događaju")
                .podnaslov(izvestaj.getDogadjajNaziv())
                .izvorOznaka(t.getIzvorOznaka())
                .ukupanPrihod(t.getUkupanPrihod())
                .ukupanTrosak(t.getUkupanTrosak())
                .neto(t.getNeto())
                .marza(t.getMarza())
                .rezultatOcene(t.getRezultatOcene())
                .prihodOdKarata(t.getPrihodOdKarata())
                .prihodOdIzlaznihFaktura(t.getPrihodOdIzlaznihFaktura())
                .trosakEvidentiran(t.getTrosakEvidentiran())
                .trosakHonorari(t.getTrosakHonorari())
                .commitovanaNabavka(t.getCommitovanaNabavka())
                .prosecnaIskoriscenostSala(r.getProsecnaIskoriscenostSala())
                .pokrivenostInventarProcenat(r.getPokrivenostInventarProcenat())
                .stavkeSala(sale)
                .stavkeOpreme(oprema)
                .stavkeNabavke(nabavke)
                .stavkeBudzeta(budzet)
                .upozorenja(r.getUpozorenja())
                .build();
    }

    private DogadjajResursiTroskoviIzvestajDto.RezimeDto buildRezime(
            DogadjajResursiTroskoviIzvestajDto.ResursiDto resursi,
            DogadjajResursiTroskoviIzvestajDto.TroskoviDto troskovi
    ) {
        return DogadjajResursiTroskoviIzvestajDto.RezimeDto.builder()
                .prosecnaIskoriscenostSala(resursi.getProsecnaIskoriscenostSala())
                .pokrivenostInventarProcenat(resursi.getPokrivenostInventarProcenat())
                .ukupanPrihod(troskovi.getUkupanPrihod())
                .ukupanTrosak(troskovi.getUkupanTrosak())
                .neto(troskovi.getNeto())
                .rezultatOcene(troskovi.getRezultatOcene())
                .brojUpozorenja(resursi.getUpozorenja().size())
                .build();
    }

    private DogadjajResursiTroskoviIzvestajDto.ResursiDto buildResursi(Long dogadjajId) {
        List<SesijaDto> sesije = sesijaService.getSesijeByDogadjaj(dogadjajId);
        Map<Long, String> lokacijaNazivi = new HashMap<>();
        List<DogadjajResursiTroskoviIzvestajDto.SalaStavkaDto> stavkeSala = new ArrayList<>();
        int sumaIskoriscenosti = 0;
        int brojSaKapacitetom = 0;

        for (SesijaDto s : sesije) {
            Integer kapacitet = s.getKapacitet();
            Integer popunjenost = s.getPopunjenost() != null ? s.getPopunjenost() : 0;
            Integer procenat = null;
            if (kapacitet != null && kapacitet > 0) {
                procenat = Math.min(100, (popunjenost * 100) / kapacitet);
                sumaIskoriscenosti += procenat;
                brojSaKapacitetom++;
            }
            String lokacija = lokacijaNazivi.computeIfAbsent(s.getLokacijaId(), id ->
                    lokacijaRepository.findById(id).map(l -> l.getNaziv()).orElse(null));
            stavkeSala.add(DogadjajResursiTroskoviIzvestajDto.SalaStavkaDto.builder()
                    .sesijaId(s.getSesijaId())
                    .sesijaNaziv(s.getNaziv())
                    .nazivSale(s.getNazivSale())
                    .lokacijaNaziv(lokacija)
                    .datum(s.getDatum() != null ? s.getDatum().toString() : null)
                    .kapacitet(kapacitet)
                    .popunjenost(popunjenost)
                    .iskoriscenostProcenat(procenat)
                    .build());
        }

        int prosecnaSala = brojSaKapacitetom > 0 ? sumaIskoriscenosti / brojSaKapacitetom : 0;

        List<DodelaOpreme> aktivneDodele = dodelaOpremeRepository.findByDogadjajWithDetalji(dogadjajId).stream()
                .filter(d -> AKTIVNE_DODELE.contains(d.getStatus()))
                .toList();

        List<DetektovanaPotrebaDto> potrebe = potrebaOpremeDetekcijaService.detektujPotrebe(dogadjajId);
        List<DogadjajResursiTroskoviIzvestajDto.OpremeStavkaDto> stavkeOpreme = new ArrayList<>();
        List<String> upozorenja = new ArrayList<>();
        int pokriveno = 0;

        for (DetektovanaPotrebaDto potreba : potrebe) {
            int dodeljeno = sumDodeljeno(aktivneDodele, potreba.getNazivResursa());
            int dostupno = opremaService.dostupnaKolicinaZaNaziv(potreba.getNazivResursa());
            int josPotrebno = Math.max(0, potreba.getKolicina() - dodeljeno);
            boolean pokrivenoStavka = josPotrebno == 0;

            if (pokrivenoStavka) {
                pokriveno++;
            } else if (dodeljeno > 0) {
                upozorenja.add(String.format(
                        "Delimično dodeljeno: %s (dodeljeno %d, potrebno %d)",
                        potreba.getNazivResursa(), dodeljeno, potreba.getKolicina()));
            } else {
                upozorenja.add(String.format(
                        "Nedovoljno na stanju: %s (potrebno %d, dostupno %d)",
                        potreba.getNazivResursa(), potreba.getKolicina(), dostupno));
            }

            stavkeOpreme.add(DogadjajResursiTroskoviIzvestajDto.OpremeStavkaDto.builder()
                    .nazivResursa(potreba.getNazivResursa())
                    .potrebnaKolicina(potreba.getKolicina())
                    .dodeljenoDogadjaju(dodeljeno)
                    .dostupnoNaStanju(dostupno)
                    .pokriveno(pokrivenoStavka)
                    .build());
        }

        if (potrebe.isEmpty()) {
            upozorenja.add("Nema detektovanih potreba za opremu ili cenovnik nije popunjen.");
        }

        int pokrivenostInventar = potrebe.isEmpty() ? 0 : (pokriveno * 100) / potrebe.size();

        List<Nabavka> nabavke = nabavkaRepository.findByDogadjajWithStavke(dogadjajId);
        List<DogadjajResursiTroskoviIzvestajDto.NabavkaStavkaDto> stavkeNabavke = nabavke.stream()
                .map(this::toNabavkaStavka)
                .toList();

        List<DodelaOpremeDto> dodeleDto = dodelaOpremeService.getByDogadjaj(dogadjajId).stream()
                .filter(d -> d.getStatus() != null && AKTIVNE_DODELE.contains(d.getStatus()))
                .toList();

        return DogadjajResursiTroskoviIzvestajDto.ResursiDto.builder()
                .prosecnaIskoriscenostSala(prosecnaSala)
                .pokrivenostInventarProcenat(pokrivenostInventar)
                .svePokrivenoInventarom(!potrebe.isEmpty() && pokriveno == potrebe.size())
                .stavkeSala(stavkeSala)
                .stavkeOpreme(stavkeOpreme)
                .aktivneDodele(dodeleDto)
                .stavkeNabavke(stavkeNabavke)
                .upozorenja(upozorenja)
                .build();
    }

    private DogadjajResursiTroskoviIzvestajDto.TroskoviDto buildTroskovi(Long dogadjajId) {
        var finalizovana = analizaProfitabilnostiRepository
                .findFirstByDogadjajDogadjajIdAndStatusOrderByFinalizovanoAtDesc(
                        dogadjajId, AnalizaStatus.FINALIZOVANA);

        BigDecimal prihodKarata = finansijskaAgregacijaService.prihodOdRegistracija(dogadjajId);
        BigDecimal prihodFakture = finansijskaAgregacijaService.prihodOdIzlaznihFaktura(dogadjajId);
        BigDecimal trosakEvidentiran = finansijskaAgregacijaService.trosakEvidentiran(dogadjajId);
        BigDecimal trosakHonorari = finansijskaAgregacijaService.trosakHonorari(dogadjajId);
        BigDecimal commitovanaNabavka = finansijskaAgregacijaService.calculateCommitovaniTrosak(dogadjajId);

        BigDecimal prihod;
        BigDecimal trosak;
        RezultatOcene rezultat;
        String izvor;

        if (finalizovana.isPresent()) {
            AnalizaProfitabilnosti analiza = finalizovana.get();
            prihod = money(analiza.getUkupanPrihod());
            trosak = money(analiza.getUkupanTrosak());
            rezultat = analiza.getRezultatOcene();
            izvor = "Finalizovano: " + (analiza.getFinalizovanoAt() != null
                    ? analiza.getFinalizovanoAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                    : "—");
        } else {
            prihod = finansijskaAgregacijaService.calculateUkupanPrihod(dogadjajId);
            trosak = finansijskaAgregacijaService.calculateUkupanTrosak(dogadjajId);
            rezultat = finansijskaAgregacijaService.classifyResult(prihod, trosak);
            izvor = "Preliminarno (nema finalizovane analize profitabilnosti)";
        }

        BigDecimal neto = prihod.subtract(trosak).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal marza = prihod.compareTo(BigDecimal.ZERO) > 0
                ? neto.divide(prihod, RATIO_SCALE, RoundingMode.HALF_EVEN)
                : null;

        List<DogadjajResursiTroskoviIzvestajDto.BudzetStavkaDto> budzetStavke = new ArrayList<>();
        for (Budzet budzet : budzetRepository.findByDogadjajIdWithDetalji(dogadjajId)) {
            if (budzet.getStavke() == null) {
                continue;
            }
            for (StavkaBudzeta sb : budzet.getStavke()) {
                BigDecimal planirano = money(sb.getPlaniraniIznos());
                BigDecimal stvarno = money(sb.getStvarniIznos());
                BigDecimal isk = planirano.compareTo(BigDecimal.ZERO) > 0
                        ? stvarno.divide(planirano, RATIO_SCALE, RoundingMode.HALF_EVEN)
                        : BigDecimal.ZERO;
                budzetStavke.add(DogadjajResursiTroskoviIzvestajDto.BudzetStavkaDto.builder()
                        .budzetId(budzet.getBudzetId())
                        .nazivBudzeta(budzet.getNazivBudzeta())
                        .kategorijaNaziv(sb.getKategorija() != null ? sb.getKategorija().getNaziv() : "—")
                        .planirano(planirano)
                        .stvarno(stvarno)
                        .iskoriscenostProcenat(isk.multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_EVEN))
                        .statusKontrole(sb.getStatusKontrole() != null ? sb.getStatusKontrole().name() : null)
                        .build());
            }
        }

        return DogadjajResursiTroskoviIzvestajDto.TroskoviDto.builder()
                .izvorOznaka(izvor)
                .ukupanPrihod(prihod)
                .ukupanTrosak(trosak)
                .neto(neto)
                .marza(marza)
                .rezultatOcene(rezultat)
                .prihodOdKarata(prihodKarata)
                .prihodOdIzlaznihFaktura(prihodFakture)
                .trosakEvidentiran(trosakEvidentiran)
                .trosakHonorari(trosakHonorari)
                .commitovanaNabavka(commitovanaNabavka)
                .iskoriscenostBudzeta(budzetStavke)
                .build();
    }

    private DogadjajResursiTroskoviIzvestajDto.NabavkaStavkaDto toNabavkaStavka(Nabavka n) {
        List<StavkaNabavke> stavke = n.getStavke() != null ? n.getStavke() : List.of();
        BigDecimal ukupno = stavke.stream()
                .map(s -> money(s.getUkupnaCena()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean commitovana = n.getStatus() != null && COMMITTED_NABAVKA.contains(n.getStatus());
        return DogadjajResursiTroskoviIzvestajDto.NabavkaStavkaDto.builder()
                .nabavkaId(n.getNabavkaId())
                .status(n.getStatus() != null ? n.getStatus().name() : null)
                .dobavljacNaziv(n.getDobavljac() != null ? n.getDobavljac().getNaziv() : null)
                .brojStavki(stavke.size())
                .ukupnaVrednost(ukupno)
                .commitovana(commitovana)
                .build();
    }

    private int sumDodeljeno(List<DodelaOpreme> dodele, String nazivPotrebe) {
        return dodele.stream()
                .filter(d -> TekstNormalizacija.naziviSePodudaraju(d.getOprema().getNaziv(), nazivPotrebe))
                .mapToInt(DodelaOpreme::getKolicina)
                .sum();
    }

    private Dogadjaj requireDogadjaj(Long dogadjajId) {
        return dogadjajRepository.findById(dogadjajId)
                .orElseThrow(() -> new NotFoundException("Događaj nije pronađen sa id: " + dogadjajId));
    }

    private BigDecimal money(BigDecimal value) {
        return finansijskaAgregacijaService.money(value);
    }
}
