package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.IzvestajPodaciDto;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.AnalizaProfitabilnosti;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.model.Klijent;
import com.eventsystem.event_management_system.model.Ugovor;
import com.eventsystem.event_management_system.repository.AnalizaProfitabilnostiRepository;
import com.eventsystem.event_management_system.repository.DobavljacRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.FakturaRepository;
import com.eventsystem.event_management_system.repository.GovornikRepository;
import com.eventsystem.event_management_system.repository.KlijentRepository;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.repository.StavkaNabavkeRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.repository.UgovorRepository;
import com.eventsystem.event_management_system.utils.enums.AnalizaStatus;
import com.eventsystem.event_management_system.utils.enums.RezultatOcene;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinansijskaAgregacijaService {

    private static final int MONEY_SCALE = 2;
    private static final int RATIO_SCALE = 4;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final List<StatusNabavke> COMMITTED_NABAVKA_STATUSES = List.of(
            StatusNabavke.POTVRDJENA,
            StatusNabavke.U_ISPORUCI,
            StatusNabavke.ZAVRSENA
    );

    private final RegistracijaRepository registracijaRepository;
    private final FakturaRepository fakturaRepository;
    private final StavkaNabavkeRepository stavkaNabavkeRepository;
    private final GovornikRepository govornikRepository;
    private final TrosakRepository trosakRepository;
    private final DogadjajRepository dogadjajRepository;
    private final KlijentRepository klijentRepository;
    private final DobavljacRepository dobavljacRepository;
    private final UgovorRepository ugovorRepository;
    private final AnalizaProfitabilnostiRepository analizaProfitabilnostiRepository;

    public BigDecimal calculateUkupanPrihod(Long dogadjajId) {
        return money(registracijaRepository.sumPrihodOdRegistracija(dogadjajId))
                .add(money(fakturaRepository.sumNetoIzlazniPrihodByDogadjaj(dogadjajId)))
                .add(sumPrihodSponzorstvaPlaceholder())
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateUkupanTrosak(Long dogadjajId) {
        return money(trosakRepository.sumNetoByDogadjaj(dogadjajId))
                .add(money(govornikRepository.sumHonorarByDogadjaj(dogadjajId)))
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateCommitovaniTrosak(Long dogadjajId) {
        return money(stavkaNabavkeRepository.sumTrosakStavkiNabavke(dogadjajId, COMMITTED_NABAVKA_STATUSES))
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    public BigDecimal prihodOdRegistracija(Long dogadjajId) {
        return money(registracijaRepository.sumPrihodOdRegistracija(dogadjajId));
    }

    public BigDecimal prihodOdIzlaznihFaktura(Long dogadjajId) {
        return money(fakturaRepository.sumNetoIzlazniPrihodByDogadjaj(dogadjajId));
    }

    public BigDecimal trosakEvidentiran(Long dogadjajId) {
        return money(trosakRepository.sumNetoByDogadjaj(dogadjajId));
    }

    public BigDecimal trosakHonorari(Long dogadjajId) {
        return money(govornikRepository.sumHonorarByDogadjaj(dogadjajId));
    }

    public BigDecimal money(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    public IzvestajPodaciDto agregacijaPoDogadjaju(Long dogadjajId) {
        Dogadjaj dogadjaj = dogadjajRepository.findById(dogadjajId)
                .orElseThrow(() -> new NotFoundException("Događaj nije pronađen sa id: " + dogadjajId));

        var finalizovana = analizaProfitabilnostiRepository
                .findFirstByDogadjajDogadjajIdAndStatusOrderByFinalizovanoAtDesc(dogadjajId, AnalizaStatus.FINALIZOVANA);

        BigDecimal prihod;
        BigDecimal trosak;
        RezultatOcene rezultat;
        String izvorOznaka;

        if (finalizovana.isPresent()) {
            AnalizaProfitabilnosti analiza = finalizovana.get();
            prihod = money(analiza.getUkupanPrihod());
            trosak = money(analiza.getUkupanTrosak());
            rezultat = analiza.getRezultatOcene();
            izvorOznaka = "Finalizovano: " + (analiza.getFinalizovanoAt() != null
                    ? analiza.getFinalizovanoAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                    : "—");
        } else {
            prihod = calculateUkupanPrihod(dogadjajId);
            trosak = calculateUkupanTrosak(dogadjajId);
            rezultat = classifyResult(prihod, trosak);
            izvorOznaka = "Preliminarno (nema finalizovane analize)";
        }

        BigDecimal neto = prihod.subtract(trosak).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal marza = prihod.compareTo(BigDecimal.ZERO) > 0
                ? neto.divide(prihod, RATIO_SCALE, RoundingMode.HALF_EVEN)
                : null;

        return IzvestajPodaciDto.builder()
                .naslov("Izveštaj po događaju")
                .podnaslov(dogadjaj.getNaziv())
                .izvorOznaka(izvorOznaka)
                .ukupanPrihod(prihod)
                .ukupanTrosak(trosak)
                .neto(neto)
                .marza(marza)
                .rezultatOcene(rezultat)
                .build();
    }

    public IzvestajPodaciDto agregacijaPoKlijentu(Long klijentId, LocalDate od, LocalDate periodDo) {
        Klijent klijent = klijentRepository.findById(klijentId)
                .orElseThrow(() -> new NotFoundException("Klijent nije pronađen sa id: " + klijentId));

        List<Faktura> fakture = fakturaRepository.findIzlazneByKlijentAndPeriod(klijentId, od, periodDo);
        BigDecimal fakturisano = money(fakturaRepository.sumUkupnaIznosByKlijentAndPeriod(klijentId, od, periodDo));
        BigDecimal naplaceno = money(fakturaRepository.sumPlaceniIznosByKlijentAndPeriod(klijentId, od, periodDo));
        BigDecimal otvoreno = fakturisano.subtract(naplaceno).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);

        String klijentNaziv = klijent.getNazivFirme() != null && !klijent.getNazivFirme().isBlank()
                ? klijent.getNazivFirme()
                : klijent.getIme() + " " + klijent.getPrezime();

        return IzvestajPodaciDto.builder()
                .naslov("Izveštaj po klijentu")
                .podnaslov(klijentNaziv.trim() + " | " + formatPeriod(od, periodDo))
                .sumaFakturisano(fakturisano)
                .sumaNaplaceno(naplaceno)
                .otvorenoPotrazivanje(otvoreno)
                .fakture(fakture.stream().map(this::toFakturaRed).toList())
                .build();
    }

    public IzvestajPodaciDto agregacijaPoDobavljacu(Long dobavljacId, LocalDate od, LocalDate periodDo) {
        Dobavljac dobavljac = dobavljacRepository.findById(dobavljacId)
                .orElseThrow(() -> new NotFoundException("Dobavljač nije pronađen sa id: " + dobavljacId));

        List<Faktura> fakture = fakturaRepository.findUlazneByDobavljacAndPeriod(dobavljacId, od, periodDo);
        BigDecimal placeno = money(fakturaRepository.sumPlaceniIznosUlazneByDobavljacAndPeriod(dobavljacId, od, periodDo));
        List<Ugovor> ugovori = ugovorRepository.findByDobavljacDobavljacId(dobavljacId);

        return IzvestajPodaciDto.builder()
                .naslov("Izveštaj po dobavljaču")
                .podnaslov(dobavljac.getNaziv() + " | " + formatPeriod(od, periodDo))
                .sumaPlaceno(placeno)
                .fakture(fakture.stream().map(this::toFakturaRed).toList())
                .ugovori(ugovori.stream().map(this::toUgovorRed).toList())
                .build();
    }

    public IzvestajPodaciDto agregacijaPoPeriodu(LocalDate od, LocalDate periodDo) {
        BigDecimal prihod = money(fakturaRepository.sumNetoIzlazniPrihodByPeriod(od, periodDo));
        BigDecimal trosak = money(trosakRepository.sumNetoByPeriod(od, periodDo));
        BigDecimal neto = prihod.subtract(trosak).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
        long brojDogadjaja = dogadjajRepository.countByDatumPocetkaBetween(od, periodDo);

        return IzvestajPodaciDto.builder()
                .naslov("Izveštaj po periodu")
                .podnaslov(formatPeriod(od, periodDo))
                .ukupanPrihod(prihod)
                .ukupanTrosak(trosak)
                .neto(neto)
                .brojDogadjaja(brojDogadjaja)
                .build();
    }

    RezultatOcene classifyResult(BigDecimal prihod, BigDecimal trosak) {
        BigDecimal safePrihod = money(prihod);
        BigDecimal safeTrosak = money(trosak);
        BigDecimal tolBe = new BigDecimal("0.01");
        if (safePrihod.compareTo(BigDecimal.ZERO) == 0) {
            return safeTrosak.compareTo(BigDecimal.ZERO) == 0
                    ? RezultatOcene.BREAK_EVEN
                    : RezultatOcene.GUBITAK;
        }
        BigDecimal neto = safePrihod.subtract(safeTrosak);
        BigDecimal tolerancija = safePrihod.multiply(tolBe);
        if (neto.compareTo(tolerancija) > 0) {
            return RezultatOcene.PROFITABILAN;
        }
        if (neto.abs().compareTo(tolerancija) <= 0) {
            return RezultatOcene.BREAK_EVEN;
        }
        return RezultatOcene.GUBITAK;
    }

    private IzvestajPodaciDto.IzvestajFakturaRedDto toFakturaRed(Faktura f) {
        return IzvestajPodaciDto.IzvestajFakturaRedDto.builder()
                .brojFakture(f.getBrojFakture())
                .datum(f.getDatumIzdavanja() != null ? f.getDatumIzdavanja().format(DATE_FMT) : "—")
                .ukupanIznos(money(f.getUkupnaIznos()))
                .placeniIznos(money(f.getPlaceniIznos()))
                .build();
    }

    private IzvestajPodaciDto.IzvestajUgovorRedDto toUgovorRed(Ugovor u) {
        return IzvestajPodaciDto.IzvestajUgovorRedDto.builder()
                .brojUgovora(u.getBrojUgovora())
                .predmet(u.getPredmet())
                .status(u.getStatus() != null ? u.getStatus().name() : "—")
                .vrednost(money(u.getVrednost()))
                .vaziDo(u.getVaziDo() != null ? u.getVaziDo().format(DATE_FMT) : "—")
                .build();
    }

    private String formatPeriod(LocalDate od, LocalDate periodDo) {
        return od.format(DATE_FMT) + " – " + periodDo.format(DATE_FMT);
    }

    private BigDecimal sumPrihodSponzorstvaPlaceholder() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }
}
