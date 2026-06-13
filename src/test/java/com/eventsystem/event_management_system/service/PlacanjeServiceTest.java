package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.PlacanjeDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.model.Placanje;
import com.eventsystem.event_management_system.model.Ugovor;
import com.eventsystem.event_management_system.repository.PlacanjeRepository;
import com.eventsystem.event_management_system.repository.UgovorRepository;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.MetodPlacanja;
import com.eventsystem.event_management_system.utils.enums.PlacanjeStatus;
import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlacanjeServiceTest {

    @Mock
    private PlacanjeRepository placanjeRepository;

    @Mock
    private FakturaService fakturaService;

    @Mock
    private UgovorRepository ugovorRepository;

    @InjectMocks
    private PlacanjeService placanjeService;

    @Test
    void create_rejectsPaymentAboveRemainingDebt() {
        Faktura faktura = Faktura.builder()
                .fakturaId(1L)
                .tip(TipFakture.IZLAZNA)
                .status(FakturaStatus.IZDATA)
                .ukupnaIznos(new BigDecimal("100.00"))
                .placeniIznos(new BigDecimal("90.00"))
                .build();
        PlacanjeDto dto = PlacanjeDto.builder()
                .iznos(new BigDecimal("20.00"))
                .metod(MetodPlacanja.GOTOVINA)
                .build();

        when(fakturaService.findEntity(1L)).thenReturn(faktura);

        assertThatThrownBy(() -> placanjeService.create(1L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("preostali dug");
    }

    @Test
    void create_rejectsDraftInvoicePayment() {
        Faktura faktura = Faktura.builder()
                .fakturaId(1L)
                .tip(TipFakture.IZLAZNA)
                .status(FakturaStatus.DRAFT)
                .ukupnaIznos(new BigDecimal("100.00"))
                .placeniIznos(BigDecimal.ZERO)
                .build();
        PlacanjeDto dto = PlacanjeDto.builder()
                .iznos(new BigDecimal("20.00"))
                .metod(MetodPlacanja.GOTOVINA)
                .build();

        when(fakturaService.findEntity(1L)).thenReturn(faktura);

        assertThatThrownBy(() -> placanjeService.create(1L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("izdate");
    }

    @Test
    void create_rejectsNonPositiveAmount() {
        Faktura faktura = Faktura.builder()
                .fakturaId(1L)
                .tip(TipFakture.IZLAZNA)
                .status(FakturaStatus.IZDATA)
                .ukupnaIznos(new BigDecimal("100.00"))
                .placeniIznos(BigDecimal.ZERO)
                .build();
        PlacanjeDto dto = PlacanjeDto.builder()
                .iznos(BigDecimal.ZERO)
                .metod(MetodPlacanja.GOTOVINA)
                .build();

        when(fakturaService.findEntity(1L)).thenReturn(faktura);

        assertThatThrownBy(() -> placanjeService.create(1L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("veći od 0");
    }

    @Test
    void confirm_rejectsPaymentThatIsNotPending() {
        Placanje placanje = Placanje.builder()
                .placanjeId(5L)
                .status(PlacanjeStatus.COMPLETED)
                .build();

        when(placanjeRepository.findById(5L)).thenReturn(Optional.of(placanje));

        assertThatThrownBy(() -> placanjeService.confirm(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void fail_rejectsPaymentThatIsNotPending() {
        Placanje placanje = Placanje.builder()
                .placanjeId(5L)
                .status(PlacanjeStatus.COMPLETED)
                .build();

        when(placanjeRepository.findById(5L)).thenReturn(Optional.of(placanje));

        assertThatThrownBy(() -> placanjeService.fail(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void create_rejectsIncomingInvoiceWhenItsOwnContractIsNotActive() {
        Faktura faktura = Faktura.builder()
                .fakturaId(1L)
                .tip(TipFakture.ULAZNA)
                .status(FakturaStatus.IZDATA)
                .ukupnaIznos(new BigDecimal("100.00"))
                .placeniIznos(BigDecimal.ZERO)
                .dobavljacId(10L)
                .ugovorId(99L)
                .build();
        Ugovor inactiveContract = Ugovor.builder()
                .ugovorId(99L)
                .dobavljac(Dobavljac.builder().dobavljacId(10L).build())
                .status(StatusUgovora.ISTEKAO)
                .vaziDo(LocalDate.now().minusDays(1))
                .build();
        PlacanjeDto dto = PlacanjeDto.builder()
                .iznos(new BigDecimal("20.00"))
                .datumPlacanja(LocalDate.now())
                .metod(MetodPlacanja.GOTOVINA)
                .build();

        when(fakturaService.findEntity(1L)).thenReturn(faktura);
        when(ugovorRepository.findById(99L)).thenReturn(Optional.of(inactiveContract));

        assertThatThrownBy(() -> placanjeService.create(1L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("konkretan aktivan ugovor");
    }
}
