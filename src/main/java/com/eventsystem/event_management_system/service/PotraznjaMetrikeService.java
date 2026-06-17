package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DetektovanaPotrebaDto;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import com.eventsystem.event_management_system.repository.StavkaNabavkeRepository;
import com.eventsystem.event_management_system.repository.TipKarteRepository;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import com.eventsystem.event_management_system.utils.enums.StatusRegistracije;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PotraznjaMetrikeService {

    private static final Set<StatusNabavke> AKTIVNE_NABAVKE = EnumSet.of(
            StatusNabavke.NACRT,
            StatusNabavke.U_OBRADI,
            StatusNabavke.PREDLOZENA,
            StatusNabavke.POTVRDJENA,
            StatusNabavke.U_ISPORUCI
    );

    private final DogadjajRepository dogadjajRepository;
    private final SesijaRepository sesijaRepository;
    private final TipKarteRepository tipKarteRepository;
    private final RegistracijaRepository registracijaRepository;
    private final StavkaNabavkeRepository stavkaNabavkeRepository;

    @Transactional(readOnly = true)
    public long danaDoDogadjaja(Long dogadjajId) {
        Dogadjaj d = dogadjajRepository.findById(dogadjajId)
                .orElseThrow(() -> new RuntimeException("Događaj nije pronađen sa id: " + dogadjajId));
        return Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), d.getDatumPocetka()));
    }

    @Transactional(readOnly = true)
    public int brojSesijaNaLokaciji(Long lokacijaId, LocalDate datum) {
        return sesijaRepository.findBySala_Lokacija_LokacijaIdAndDatumBetween(lokacijaId, datum, datum).size();
    }

    @Transactional(readOnly = true)
    public int zauzetostSaleProcenat(Long lokacijaId, LocalDate datum) {
        int sesija = brojSesijaNaLokaciji(lokacijaId, datum);
        // Heuristika: 4+ sesije na lokaciji istog dana = visoka potražnja (100%)
        return Math.min(100, sesija * 25);
    }

    @Transactional(readOnly = true)
    public int prodatoKarata(Long dogadjajId, String nazivTipa) {
        return (int) registracijaRepository.countByTipKarteIdDogadjajIdAndTipKarteIdNazivTipaAndStatus(
                dogadjajId, nazivTipa, StatusRegistracije.POTVRDJENA);
    }

    @Transactional(readOnly = true)
    public int ukupnaKvotaDogadjaja(Long dogadjajId) {
        return tipKarteRepository.findAllByDogadjaj_DogadjajId(dogadjajId).stream()
                .mapToInt(t -> t.getKvota() != null ? t.getKvota() : 0)
                .sum();
    }

    @Transactional(readOnly = true)
    public int ukupnoProdatoDogadjaja(Long dogadjajId) {
        return (int) registracijaRepository.countByTipKarteIdDogadjajIdAndStatus(
                dogadjajId, StatusRegistracije.POTVRDJENA);
    }

    /**
     * Ukupna tražena količina resursa u aktivnim nabavkama širom projekta.
     */
    @Transactional(readOnly = true)
    public int ukupnaPotraznjaKolicina(Long cenovnikId, String nazivResursa) {
        return stavkaNabavkeRepository.sumPotraznjaKolicina(cenovnikId, nazivResursa, AKTIVNE_NABAVKE);
    }

    /**
     * Broj različitih događaja koji trenutno traže ovaj resurs (aktivne nabavke).
     */
    @Transactional(readOnly = true)
    public int brojDogadjajaSaPotrebom(Long cenovnikId, String nazivResursa) {
        return stavkaNabavkeRepository.countDogadjajiSaPotrebom(cenovnikId, nazivResursa, AKTIVNE_NABAVKE);
    }

    /**
     * Skor potražnje 0–100 u odnosu na najtraženiji resurs u projektu.
     */
    @Transactional(readOnly = true)
    public int potraznjaIndeksProcenat(Long cenovnikId, String nazivResursa, int maxPotraznja) {
        if (maxPotraznja <= 0) {
            return 0;
        }
        int kolicina = ukupnaPotraznjaKolicina(cenovnikId, nazivResursa);
        return Math.min(100, (kolicina * 100) / maxPotraznja);
    }
}
