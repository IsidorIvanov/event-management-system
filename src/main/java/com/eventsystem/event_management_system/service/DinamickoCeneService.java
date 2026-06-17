package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.*;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.*;
import com.eventsystem.event_management_system.model.compositePK.SalaId;
import com.eventsystem.event_management_system.model.compositePK.TipKarteId;
import com.eventsystem.event_management_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dinamičko određivanje cena na osnovu potražnje.
 *
 * Pravila za karte:
 * - &gt;80% prodato → +10% (visoka potražnja)
 * - &lt;30% prodato i &lt;7 dana do događaja → -15% (last minute)
 * - &lt;20% prodato i ≥30 dana do događaja → -5% (early bird)
 *
 * Pravila za sale:
 * - zauzetost lokacije &gt;50% → +10% na baznu cenu po danu
 * - zauzetost &gt;75% → +20%
 *
 * Pravila za cenovnik (potražnja na nivou projekta):
 * - indeks potražnje &gt;= 70% → +10%
 * - indeks &lt;= 25% i količina &lt;= 1 → -5%
 * - inače bez promene
 */
@Service
@RequiredArgsConstructor
public class DinamickoCeneService {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final DogadjajRepository dogadjajRepository;
    private final TipKarteRepository tipKarteRepository;
    private final SalaRepository salaRepository;
    private final PotraznjaMetrikeService potraznjaMetrikeService;

    @Transactional(readOnly = true)
    public PredlogCeneDogadjajaDto predlogCenaKarata(Long dogadjajId) {
        Dogadjaj dogadjaj = dogadjajRepository.findById(dogadjajId)
                .orElseThrow(() -> new NotFoundException("Događaj nije pronađen sa id: " + dogadjajId));

        List<TipKarte> tipovi = tipKarteRepository.findAllByDogadjaj_DogadjajId(dogadjajId);
        long danaDo = potraznjaMetrikeService.danaDoDogadjaja(dogadjajId);
        int ukupnaKvota = potraznjaMetrikeService.ukupnaKvotaDogadjaja(dogadjajId);
        int ukupnoProdato = potraznjaMetrikeService.ukupnoProdatoDogadjaja(dogadjajId);
        int popunjenostUkupno = ukupnaKvota > 0 ? (ukupnoProdato * 100) / ukupnaKvota : 0;

        List<PredlogCeneKarteDto> predlozi = new ArrayList<>();
        for (TipKarte tip : tipovi) {
            predlozi.add(izracunajPredlogKarte(tip, danaDo));
        }

        return PredlogCeneDogadjajaDto.builder()
                .dogadjajId(dogadjajId)
                .ukupnaKvota(ukupnaKvota)
                .ukupnoProdato(ukupnoProdato)
                .popunjenostProcenat(popunjenostUkupno)
                .danaDoDogadjaja(danaDo)
                .tipoviKarata(predlozi)
                .build();
    }

    @Transactional
    public PredlogCeneKarteDto primeniCenuKarte(Long dogadjajId, String nazivTipa, BigDecimal novaCena) {
        if (novaCena == null || novaCena.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Cena mora biti nula ili pozitivna.");
        }
        TipKarteId id = new TipKarteId(dogadjajId, nazivTipa);
        TipKarte tip = tipKarteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tip karte nije pronađen."));
        tip.setCena(novaCena.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN));
        tipKarteRepository.save(tip);
        return izracunajPredlogKarte(tip, potraznjaMetrikeService.danaDoDogadjaja(dogadjajId));
    }

    /**
     * Predlog nove cene za stavku cenovnika na osnovu potražnje u celom projektu.
     */
    @Transactional(readOnly = true)
    public PredlogCeneCenovnikaDto predlogCeneCenovnika(Cenovnik c, int maxPotraznjaUProjektu) {
        int kolicina = potraznjaMetrikeService.ukupnaPotraznjaKolicina(c.getCenovnikId(), c.getNazivResursa());
        int brojDogadjaja = potraznjaMetrikeService.brojDogadjajaSaPotrebom(c.getCenovnikId(), c.getNazivResursa());
        int indeks = potraznjaMetrikeService.potraznjaIndeksProcenat(
                c.getCenovnikId(), c.getNazivResursa(), maxPotraznjaUProjektu);

        BigDecimal multiplikator = BigDecimal.ONE;
        String pravilo = String.format(
                "Stabilna potražnja (indeks %d%%, %d kom u projektu, %d događaja)",
                indeks, kolicina, brojDogadjaja);

        if (indeks >= 70) {
            multiplikator = BigDecimal.valueOf(1.10);
            pravilo = String.format(
                    "Visoka potražnja u projektu (indeks %d%%, %d kom, %d događaja) → +10%%",
                    indeks, kolicina, brojDogadjaja);
        } else if (indeks <= 25 && kolicina <= 1) {
            multiplikator = BigDecimal.valueOf(0.95);
            pravilo = String.format(
                    "Niska potražnja u projektu (indeks %d%%, %d kom) → -5%%",
                    indeks, kolicina);
        }

        BigDecimal predlozena = c.getCenaJedinicna().multiply(multiplikator)
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);

        return PredlogCeneCenovnikaDto.builder()
                .cenovnikId(c.getCenovnikId())
                .nazivResursa(c.getNazivResursa())
                .dobavljacId(c.getDobavljac().getDobavljacId())
                .dobavljacNaziv(c.getDobavljac().getNaziv())
                .trenutnaCena(c.getCenaJedinicna())
                .predlozenaCena(predlozena)
                .ukupnaPotraznjaKolicina(kolicina)
                .brojDogadjajaSaPotrebom(brojDogadjaja)
                .potraznjaIndeksProcenat(indeks)
                .pravilo(pravilo)
                .promenaProcenat(multiplikator.subtract(BigDecimal.ONE).multiply(HUNDRED)
                        .setScale(0, RoundingMode.HALF_EVEN))
                .build();
    }

    @Transactional(readOnly = true)
    public int maxPotraznjaKolicinaProjekta(List<Cenovnik> stavke) {
        return stavke.stream()
                .mapToInt(c -> potraznjaMetrikeService.ukupnaPotraznjaKolicina(
                        c.getCenovnikId(), c.getNazivResursa()))
                .max()
                .orElse(0);
    }

    public CenovnikDto dopuniPredlogCene(Cenovnik entity, CenovnikDto dto, int maxPotraznjaUProjektu) {
        PredlogCeneCenovnikaDto predlog = predlogCeneCenovnika(entity, maxPotraznjaUProjektu);
        dto.setPredlozenaCena(predlog.getPredlozenaCena());
        dto.setPotraznjaIndeksProcenat(predlog.getPotraznjaIndeksProcenat());
        dto.setUkupnaPotraznjaKolicina(predlog.getUkupnaPotraznjaKolicina());
        dto.setBrojDogadjajaSaPotrebom(predlog.getBrojDogadjajaSaPotrebom());
        dto.setPraviloPredloga(predlog.getPravilo());
        return dto;
    }

    @Transactional
    public PredlogCeneSaleDto primeniCenuSale(Long lokacijaId, String nazivSale, LocalDate datum) {
        PredlogCeneSaleDto predlog = predlogCeneSale(lokacijaId, nazivSale, datum);
        SalaId id = new SalaId(lokacijaId, nazivSale);
        Sala sala = salaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sala nije pronađena."));
        sala.setBaznaCenaPoDanu(predlog.getPredlozenaCenaPoDanu());
        salaRepository.save(sala);
        return predlogCeneSale(lokacijaId, nazivSale, datum);
    }

    @Transactional(readOnly = true)
    public PredlogCeneSaleDto predlogCeneSale(Long lokacijaId, String nazivSale, LocalDate datum) {
        SalaId id = new SalaId(lokacijaId, nazivSale);
        Sala sala = salaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sala nije pronađena."));

        int zauzetost = potraznjaMetrikeService.zauzetostSaleProcenat(lokacijaId, datum);
        int brojSesija = potraznjaMetrikeService.brojSesijaNaLokaciji(lokacijaId, datum);

        BigDecimal multiplikator;
        String pravilo;
        if (zauzetost > 75) {
            multiplikator = BigDecimal.valueOf(1.20);
            pravilo = "Visoka zauzetost lokacije (>75%) → +20%";
        } else if (zauzetost > 50) {
            multiplikator = BigDecimal.valueOf(1.10);
            pravilo = "Povećana potražnja (>50%) → +10%";
        } else {
            multiplikator = BigDecimal.ONE;
            pravilo = "Standardna potražnja — bazna cena";
        }

        BigDecimal predlozena = sala.getBaznaCenaPoDanu().multiply(multiplikator)
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
        BigDecimal promena = multiplikator.subtract(BigDecimal.ONE).multiply(HUNDRED)
                .setScale(0, RoundingMode.HALF_EVEN);

        return PredlogCeneSaleDto.builder()
                .lokacijaId(lokacijaId)
                .nazivSale(nazivSale)
                .datum(datum)
                .baznaCenaPoDanu(sala.getBaznaCenaPoDanu())
                .predlozenaCenaPoDanu(predlozena)
                .zauzetostProcenat(zauzetost)
                .brojSesijaNaLokaciji(brojSesija)
                .pravilo(pravilo)
                .promenaProcenat(promena)
                .build();
    }

    private PredlogCeneKarteDto izracunajPredlogKarte(TipKarte tip, long danaDo) {
        Long dogadjajId = tip.getId().getDogadjajId();
        String nazivTipa = tip.getId().getNazivTipa();
        int prodato = potraznjaMetrikeService.prodatoKarata(dogadjajId, nazivTipa);
        int kvota = tip.getKvota() != null ? tip.getKvota() : 0;
        int popunjenost = kvota > 0 ? (prodato * 100) / kvota : 0;

        BigDecimal multiplikator = BigDecimal.ONE;
        String pravilo = "Bez promene — stabilna potražnja";

        if (popunjenost > 80) {
            multiplikator = BigDecimal.valueOf(1.10);
            pravilo = "Visoka potražnja (>80% prodato) → +10%";
        } else if (popunjenost < 30 && danaDo < 7) {
            multiplikator = BigDecimal.valueOf(0.85);
            pravilo = "Niska prodaja blizu termina (<30%, <7 dana) → -15%";
        } else if (popunjenost < 20 && danaDo >= 30) {
            multiplikator = BigDecimal.valueOf(0.95);
            pravilo = "Rani period (<20% prodato, ≥30 dana) → -5% early bird";
        }

        BigDecimal predlozena = tip.getCena().multiply(multiplikator)
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);

        return PredlogCeneKarteDto.builder()
                .dogadjajId(dogadjajId)
                .nazivTipa(nazivTipa)
                .trenutnaCena(tip.getCena())
                .predlozenaCena(predlozena)
                .kvota(kvota)
                .prodato(prodato)
                .popunjenostProcenat(popunjenost)
                .danaDoDogadjaja(danaDo)
                .pravilo(pravilo)
                .promenaProcenat(multiplikator.subtract(BigDecimal.ONE).multiply(HUNDRED)
                        .setScale(0, RoundingMode.HALF_EVEN))
                .build();
    }
}
