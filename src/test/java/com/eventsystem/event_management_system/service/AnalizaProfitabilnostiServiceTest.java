package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.AnalizaProfitabilnostiDto;
import com.eventsystem.event_management_system.dto.CreateAnalizaProfitabilnostiRequest;
import com.eventsystem.event_management_system.dto.UpdateAnalizaProfitabilnostiRequest;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.AnalizaProfitabilnosti;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.repository.AnalizaProfitabilnostiRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.FakturaRepository;
import com.eventsystem.event_management_system.repository.GovornikRepository;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.repository.StavkaNabavkeRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.AnalizaStatus;
import com.eventsystem.event_management_system.utils.enums.RezultatOcene;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalizaProfitabilnostiServiceTest {

    @Mock
    private AnalizaProfitabilnostiRepository analizaProfitabilnostiRepository;

    @Mock
    private DogadjajRepository dogadjajRepository;

    @Mock
    private RegistracijaRepository registracijaRepository;

    @Mock
    private FakturaRepository fakturaRepository;

    @Mock
    private StavkaNabavkeRepository stavkaNabavkeRepository;

    @Mock
    private GovornikRepository govornikRepository;

    @Mock
    private TrosakRepository trosakRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AnalizaProfitabilnostiService analizaProfitabilnostiService;

    @Test
    void createDraft_rejectsEventThatIsNotFinished() {
        when(dogadjajRepository.findById(10L)).thenReturn(Optional.of(dogadjaj(StatusDogadjaja.AKTIVAN)));

        assertThatThrownBy(() -> analizaProfitabilnostiService.createDraft(10L, createRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("završen");
    }

    @Test
    void createDraft_aggregatesImplementedSourcesAndPlaceholderZeros() {
        when(dogadjajRepository.findById(10L)).thenReturn(Optional.of(dogadjaj(StatusDogadjaja.ZAVRSEN)));
        when(currentUserService.getCurrentZaposleni()).thenReturn(zaposleni(7L));
        when(registracijaRepository.sumPrihodOdRegistracija(10L)).thenReturn(new BigDecimal("1000.00"));
        when(fakturaRepository.sumNetoIzlazniPrihodByDogadjaj(10L)).thenReturn(new BigDecimal("250.50"));
        when(stavkaNabavkeRepository.sumTrosakStavkiNabavke(any(), any())).thenReturn(new BigDecimal("400.00"));
        when(govornikRepository.sumHonorarByDogadjaj(10L)).thenReturn(new BigDecimal("150.00"));
        when(trosakRepository.sumNetoByDogadjaj(10L)).thenReturn(new BigDecimal("75.25"));
        when(analizaProfitabilnostiRepository.save(any(AnalizaProfitabilnosti.class))).thenAnswer(invocation -> {
            AnalizaProfitabilnosti analiza = invocation.getArgument(0);
            analiza.setAnalizaId(99L);
            return analiza;
        });

        AnalizaProfitabilnostiDto dto = analizaProfitabilnostiService.createDraft(10L, createRequest());

        assertThat(dto.getAnalizaId()).isEqualTo(99L);
        assertThat(dto.getUkupanPrihod()).isEqualByComparingTo("1250.50");
        assertThat(dto.getUkupanTrosak()).isEqualByComparingTo("625.25");
        assertThat(dto.getNeto()).isEqualByComparingTo("625.25");
        assertThat(dto.getMarza()).isEqualByComparingTo("0.5000");
        assertThat(dto.getRoi()).isEqualByComparingTo("1.0000");
        assertThat(dto.getStatus()).isEqualTo(AnalizaStatus.DRAFT);
        assertThat(dto.getRezultatOcene()).isNull();
        assertThat(dto.getNapomene()).isEqualTo("Snapshot za odbranu");
    }

    @Test
    void classifyResult_returnsProfitableWhenNetIsGreaterThanOnePercentRevenue() {
        RezultatOcene result = analizaProfitabilnostiService.classifyResult(
                new BigDecimal("1000.00"),
                new BigDecimal("980.00")
        );

        assertThat(result).isEqualTo(RezultatOcene.PROFITABILAN);
    }

    @Test
    void classifyResult_returnsBreakEvenWithinOnePercentRevenue() {
        RezultatOcene result = analizaProfitabilnostiService.classifyResult(
                new BigDecimal("1000.00"),
                new BigDecimal("1009.99")
        );

        assertThat(result).isEqualTo(RezultatOcene.BREAK_EVEN);
    }

    @Test
    void classifyResult_returnsLossWhenNetIsBelowNegativeTolerance() {
        RezultatOcene result = analizaProfitabilnostiService.classifyResult(
                new BigDecimal("1000.00"),
                new BigDecimal("1010.01")
        );

        assertThat(result).isEqualTo(RezultatOcene.GUBITAK);
    }

    @Test
    void classifyResult_returnsBreakEvenWhenRevenueAndCostAreZero() {
        RezultatOcene result = analizaProfitabilnostiService.classifyResult(BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(result).isEqualTo(RezultatOcene.BREAK_EVEN);
    }

    @Test
    void classifyResult_returnsLossWhenRevenueIsZeroAndCostIsPositive() {
        RezultatOcene result = analizaProfitabilnostiService.classifyResult(BigDecimal.ZERO, new BigDecimal("1.00"));

        assertThat(result).isEqualTo(RezultatOcene.GUBITAK);
    }

    @Test
    void finalize_rejectsAnalysisThatIsNotDraft() {
        when(analizaProfitabilnostiRepository.findByIdWithDetalji(5L))
                .thenReturn(Optional.of(analiza(AnalizaStatus.FINALIZOVANA, zaposleni(1L))));

        assertThatThrownBy(() -> analizaProfitabilnostiService.finalize(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("draft");
    }

    @Test
    void finalize_rejectsSameCreatorAndFinalizerForBr03() {
        when(analizaProfitabilnostiRepository.findByIdWithDetalji(5L))
                .thenReturn(Optional.of(analiza(AnalizaStatus.DRAFT, zaposleni(7L))));
        when(currentUserService.getCurrentZaposleni()).thenReturn(zaposleni(7L));

        assertThatThrownBy(() -> analizaProfitabilnostiService.finalize(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Kreator analize");
    }

    @Test
    void updateDraft_rejectsFinalizedAnalysis() {
        when(analizaProfitabilnostiRepository.findByIdWithDetalji(5L))
                .thenReturn(Optional.of(analiza(AnalizaStatus.FINALIZOVANA, zaposleni(1L))));

        assertThatThrownBy(() -> analizaProfitabilnostiService.updateDraft(5L, updateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Finalizovana");
    }

    private CreateAnalizaProfitabilnostiRequest createRequest() {
        return CreateAnalizaProfitabilnostiRequest.builder()
                .napomene("  Snapshot za odbranu  ")
                .build();
    }

    private UpdateAnalizaProfitabilnostiRequest updateRequest() {
        return UpdateAnalizaProfitabilnostiRequest.builder()
                .napomene("Izmena napomene")
                .build();
    }

    private Dogadjaj dogadjaj(StatusDogadjaja status) {
        return Dogadjaj.builder()
                .dogadjajId(10L)
                .naziv("Završna konferencija")
                .status(status)
                .build();
    }

    private Zaposleni zaposleni(Long id) {
        Zaposleni zaposleni = new Zaposleni();
        zaposleni.setKorisnikId(id);
        zaposleni.setIme("Test");
        zaposleni.setPrezime("Korisnik");
        zaposleni.setEmail("test-" + id + "@example.com");
        zaposleni.setLozinkaHash("hash");
        return zaposleni;
    }

    private AnalizaProfitabilnosti analiza(AnalizaStatus status, Zaposleni kreator) {
        return AnalizaProfitabilnosti.builder()
                .analizaId(5L)
                .dogadjaj(dogadjaj(StatusDogadjaja.ZAVRSEN))
                .kreirao(kreator)
                .ukupanPrihod(new BigDecimal("1000.00"))
                .ukupanTrosak(new BigDecimal("900.00"))
                .datumAnalize(LocalDate.now())
                .status(status)
                .build();
    }
}
