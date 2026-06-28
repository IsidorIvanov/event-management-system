package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.IzvestajPodaciDto;
import com.eventsystem.event_management_system.model.AnalizaProfitabilnosti;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Klijent;
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
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinansijskaAgregacijaServiceTest {

    @Mock private RegistracijaRepository registracijaRepository;
    @Mock private FakturaRepository fakturaRepository;
    @Mock private StavkaNabavkeRepository stavkaNabavkeRepository;
    @Mock private GovornikRepository govornikRepository;
    @Mock private TrosakRepository trosakRepository;
    @Mock private DogadjajRepository dogadjajRepository;
    @Mock private KlijentRepository klijentRepository;
    @Mock private DobavljacRepository dobavljacRepository;
    @Mock private UgovorRepository ugovorRepository;
    @Mock private AnalizaProfitabilnostiRepository analizaProfitabilnostiRepository;

    private FinansijskaAgregacijaService service;

    @BeforeEach
    void setUp() {
        service = new FinansijskaAgregacijaService(
                registracijaRepository,
                fakturaRepository,
                stavkaNabavkeRepository,
                govornikRepository,
                trosakRepository,
                dogadjajRepository,
                klijentRepository,
                dobavljacRepository,
                ugovorRepository,
                analizaProfitabilnostiRepository
        );
    }

    @Test
    void agregacijaPoKlijentu_calculatesSumsAndOpenReceivables() {
        LocalDate od = LocalDate.of(2026, 1, 1);
        LocalDate periodDo = LocalDate.of(2026, 1, 31);
        Klijent klijent = new Klijent();
        klijent.setKorisnikId(5L);
        klijent.setIme("Marko");
        klijent.setPrezime("Marković");
        klijent.setNazivFirme("Firma DOO");

        when(klijentRepository.findById(5L)).thenReturn(Optional.of(klijent));
        when(fakturaRepository.findIzlazneByKlijentAndPeriod(5L, od, periodDo)).thenReturn(List.of());
        when(fakturaRepository.sumUkupnaIznosByKlijentAndPeriod(5L, od, periodDo)).thenReturn(new BigDecimal("1000.00"));
        when(fakturaRepository.sumPlaceniIznosByKlijentAndPeriod(5L, od, periodDo)).thenReturn(new BigDecimal("600.00"));

        IzvestajPodaciDto dto = service.agregacijaPoKlijentu(5L, od, periodDo);

        assertThat(dto.getSumaFakturisano()).isEqualByComparingTo("1000.00");
        assertThat(dto.getSumaNaplaceno()).isEqualByComparingTo("600.00");
        assertThat(dto.getOtvorenoPotrazivanje()).isEqualByComparingTo("400.00");
    }

    @Test
    void agregacijaPoDobavljacu_sumsPlaceno() {
        LocalDate od = LocalDate.of(2026, 2, 1);
        LocalDate periodDo = LocalDate.of(2026, 2, 28);
        Dobavljac dobavljac = Dobavljac.builder().dobavljacId(3L).naziv("Dobavljac ABC").build();

        when(dobavljacRepository.findById(3L)).thenReturn(Optional.of(dobavljac));
        when(fakturaRepository.findUlazneByDobavljacAndPeriod(3L, od, periodDo)).thenReturn(List.of());
        when(fakturaRepository.sumPlaceniIznosUlazneByDobavljacAndPeriod(3L, od, periodDo))
                .thenReturn(new BigDecimal("250.50"));
        when(ugovorRepository.findByDobavljacDobavljacId(3L)).thenReturn(List.of());

        IzvestajPodaciDto dto = service.agregacijaPoDobavljacu(3L, od, periodDo);

        assertThat(dto.getSumaPlaceno()).isEqualByComparingTo("250.50");
    }

    @Test
    void agregacijaPoPeriodu_usesDatumTroskaForCosts() {
        LocalDate od = LocalDate.of(2026, 3, 1);
        LocalDate periodDo = LocalDate.of(2026, 3, 31);

        when(fakturaRepository.sumNetoIzlazniPrihodByPeriod(od, periodDo)).thenReturn(new BigDecimal("5000.00"));
        when(trosakRepository.sumNetoByPeriod(od, periodDo)).thenReturn(new BigDecimal("1200.00"));
        when(dogadjajRepository.countByDatumPocetkaBetween(od, periodDo)).thenReturn(4L);

        IzvestajPodaciDto dto = service.agregacijaPoPeriodu(od, periodDo);

        verify(trosakRepository).sumNetoByPeriod(eq(od), eq(periodDo));
        assertThat(dto.getUkupanPrihod()).isEqualByComparingTo("5000.00");
        assertThat(dto.getUkupanTrosak()).isEqualByComparingTo("1200.00");
        assertThat(dto.getNeto()).isEqualByComparingTo("3800.00");
        assertThat(dto.getBrojDogadjaja()).isEqualTo(4L);
    }

    @Test
    void agregacijaPoDogadjaju_usesFinalizedAnalysisWhenPresent() {
        Dogadjaj dogadjaj = Dogadjaj.builder().dogadjajId(10L).naziv("Konferencija").status(StatusDogadjaja.ZAVRSEN).build();
        AnalizaProfitabilnosti analiza = AnalizaProfitabilnosti.builder()
                .ukupanPrihod(new BigDecimal("10000.00"))
                .ukupanTrosak(new BigDecimal("7000.00"))
                .rezultatOcene(RezultatOcene.PROFITABILAN)
                .status(AnalizaStatus.FINALIZOVANA)
                .build();

        when(dogadjajRepository.findById(10L)).thenReturn(Optional.of(dogadjaj));
        when(analizaProfitabilnostiRepository.findFirstByDogadjajDogadjajIdAndStatusOrderByFinalizovanoAtDesc(
                10L, AnalizaStatus.FINALIZOVANA)).thenReturn(Optional.of(analiza));

        IzvestajPodaciDto dto = service.agregacijaPoDogadjaju(10L);

        assertThat(dto.getUkupanPrihod()).isEqualByComparingTo("10000.00");
        assertThat(dto.getUkupanTrosak()).isEqualByComparingTo("7000.00");
        assertThat(dto.getRezultatOcene()).isEqualTo(RezultatOcene.PROFITABILAN);
        assertThat(dto.getIzvorOznaka()).startsWith("Finalizovano:");
    }

    @Test
    void agregacijaPoDogadjaju_usesLiveAggregationWhenNoFinalizedAnalysis() {
        Dogadjaj dogadjaj = Dogadjaj.builder().dogadjajId(10L).naziv("Konferencija").status(StatusDogadjaja.ZAVRSEN).build();

        when(dogadjajRepository.findById(10L)).thenReturn(Optional.of(dogadjaj));
        when(analizaProfitabilnostiRepository.findFirstByDogadjajDogadjajIdAndStatusOrderByFinalizovanoAtDesc(
                10L, AnalizaStatus.FINALIZOVANA)).thenReturn(Optional.empty());
        when(registracijaRepository.sumPrihodOdRegistracija(10L)).thenReturn(new BigDecimal("500.00"));
        when(fakturaRepository.sumNetoIzlazniPrihodByDogadjaj(10L)).thenReturn(BigDecimal.ZERO);
        when(trosakRepository.sumNetoByDogadjaj(10L)).thenReturn(new BigDecimal("100.00"));
        when(govornikRepository.sumHonorarByDogadjaj(10L)).thenReturn(BigDecimal.ZERO);

        IzvestajPodaciDto dto = service.agregacijaPoDogadjaju(10L);

        assertThat(dto.getUkupanPrihod()).isEqualByComparingTo("500.00");
        assertThat(dto.getUkupanTrosak()).isEqualByComparingTo("100.00");
        assertThat(dto.getIzvorOznaka()).contains("Preliminarno");
    }
}
