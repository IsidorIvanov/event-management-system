package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DetektovanaPotrebaDto;
import com.eventsystem.event_management_system.dto.DogadjajResursiTroskoviIzvestajDto;
import com.eventsystem.event_management_system.dto.SesijaDto;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.DodelaOpreme;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.model.Oprema;
import com.eventsystem.event_management_system.repository.AnalizaProfitabilnostiRepository;
import com.eventsystem.event_management_system.repository.BudzetRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.DodelaOpremeRepository;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import com.eventsystem.event_management_system.repository.NabavkaRepository;
import com.eventsystem.event_management_system.utils.enums.RezultatOcene;
import com.eventsystem.event_management_system.utils.enums.StatusDodeleOpreme;
import com.eventsystem.event_management_system.utils.enums.TipSesije;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DogadjajIzvestajServiceTest {

    @Mock private DogadjajRepository dogadjajRepository;
    @Mock private SesijaService sesijaService;
    @Mock private PotrebaOpremeDetekcijaService potrebaOpremeDetekcijaService;
    @Mock private OpremaService opremaService;
    @Mock private DodelaOpremeRepository dodelaOpremeRepository;
    @Mock private DodelaOpremeService dodelaOpremeService;
    @Mock private NabavkaRepository nabavkaRepository;
    @Mock private FinansijskaAgregacijaService finansijskaAgregacijaService;
    @Mock private BudzetRepository budzetRepository;
    @Mock private AnalizaProfitabilnostiRepository analizaProfitabilnostiRepository;
    @Mock private LokacijaRepository lokacijaRepository;

    private DogadjajIzvestajService service;

    @BeforeEach
    void setUp() {
        service = new DogadjajIzvestajService(
                dogadjajRepository,
                sesijaService,
                potrebaOpremeDetekcijaService,
                opremaService,
                dodelaOpremeRepository,
                dodelaOpremeService,
                nabavkaRepository,
                finansijskaAgregacijaService,
                budzetRepository,
                analizaProfitabilnostiRepository,
                lokacijaRepository
        );
    }

    @Test
    void getKompletanIzvestaj_aggregatesResursiAndTroskovi() {
        Dogadjaj dogadjaj = Dogadjaj.builder().dogadjajId(1L).naziv("GamesCon").build();
        when(dogadjajRepository.findById(1L)).thenReturn(Optional.of(dogadjaj));

        SesijaDto sesija = new SesijaDto(
                1L, 10L, "Sala A", "Keynote",
                LocalDate.of(2026, 6, 1),
                LocalTime.of(10, 0), LocalTime.of(11, 0),
                TipSesije.KEYNOTE, 100, null
        );
        sesija.setSesijaId(5L);
        sesija.setPopunjenost(75);
        when(sesijaService.getSesijeByDogadjaj(1L)).thenReturn(List.of(sesija));

        Lokacija lokacija = Lokacija.builder().lokacijaId(10L).naziv("Beogradski sajam").build();
        when(lokacijaRepository.findById(10L)).thenReturn(Optional.of(lokacija));

        when(potrebaOpremeDetekcijaService.detektujPotrebe(1L)).thenReturn(List.of(
                DetektovanaPotrebaDto.builder().nazivResursa("Zvučnici PA").kolicina(2).build()
        ));
        when(opremaService.dostupnaKolicinaZaNaziv("Zvučnici PA")).thenReturn(1);

        Oprema oprema = Oprema.builder().naziv("Zvučnici PA").build();
        DodelaOpreme dodela = DodelaOpreme.builder()
                .oprema(oprema)
                .kolicina(2)
                .status(StatusDodeleOpreme.REZERVISANO)
                .build();
        when(dodelaOpremeRepository.findByDogadjajWithDetalji(1L)).thenReturn(List.of(dodela));
        when(dodelaOpremeService.getByDogadjaj(1L)).thenReturn(List.of());
        when(nabavkaRepository.findByDogadjajWithStavke(1L)).thenReturn(List.of());

        when(analizaProfitabilnostiRepository.findFirstByDogadjajDogadjajIdAndStatusOrderByFinalizovanoAtDesc(
                anyLong(), any())).thenReturn(Optional.empty());
        when(finansijskaAgregacijaService.calculateUkupanPrihod(1L)).thenReturn(new BigDecimal("100000"));
        when(finansijskaAgregacijaService.calculateUkupanTrosak(1L)).thenReturn(new BigDecimal("60000"));
        when(finansijskaAgregacijaService.classifyResult(any(), any())).thenReturn(RezultatOcene.PROFITABILAN);
        when(finansijskaAgregacijaService.prihodOdRegistracija(1L)).thenReturn(new BigDecimal("80000"));
        when(finansijskaAgregacijaService.prihodOdIzlaznihFaktura(1L)).thenReturn(new BigDecimal("20000"));
        when(finansijskaAgregacijaService.trosakEvidentiran(1L)).thenReturn(new BigDecimal("50000"));
        when(finansijskaAgregacijaService.trosakHonorari(1L)).thenReturn(new BigDecimal("10000"));
        when(finansijskaAgregacijaService.calculateCommitovaniTrosak(1L)).thenReturn(new BigDecimal("15000"));
        when(budzetRepository.findByDogadjajIdWithDetalji(1L)).thenReturn(List.of());

        DogadjajResursiTroskoviIzvestajDto izvestaj = service.getKompletanIzvestaj(1L);

        assertThat(izvestaj.getDogadjajNaziv()).isEqualTo("GamesCon");
        assertThat(izvestaj.getResursi().getProsecnaIskoriscenostSala()).isEqualTo(75);
        assertThat(izvestaj.getResursi().getPokrivenostInventarProcenat()).isEqualTo(100);
        assertThat(izvestaj.getResursi().getStavkeOpreme().get(0).isPokriveno()).isTrue();
        assertThat(izvestaj.getTroskovi().getNeto()).isEqualByComparingTo("40000.00");
        assertThat(izvestaj.getRezime().getBrojUpozorenja()).isZero();
    }
}
