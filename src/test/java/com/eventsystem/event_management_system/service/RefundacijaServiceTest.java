package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.RefundacijaDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.model.Placanje;
import com.eventsystem.event_management_system.model.Refundacija;
import com.eventsystem.event_management_system.model.StavkaFakture;
import com.eventsystem.event_management_system.model.Trosak;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.repository.PlacanjeRepository;
import com.eventsystem.event_management_system.repository.RefundacijaRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.PlacanjeStatus;
import com.eventsystem.event_management_system.utils.enums.RefundacijaStatus;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
import com.eventsystem.event_management_system.utils.enums.TipTroska;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundacijaServiceTest {

    @Mock
    private RefundacijaRepository refundacijaRepository;

    @Mock
    private PlacanjeRepository placanjeRepository;

    @Mock
    private FakturaService fakturaService;

    @Mock
    private TrosakRepository trosakRepository;

    @Mock
    private BudzetService budzetService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private RefundacijaService refundacijaService;

    @Test
    void request_rejectsRefundForNonCompletedPayment() {
        Placanje placanje = Placanje.builder()
                .placanjeId(5L)
                .status(PlacanjeStatus.PENDING)
                .iznos(new BigDecimal("100.00"))
                .build();
        RefundacijaDto dto = RefundacijaDto.builder()
                .iznos(new BigDecimal("10.00"))
                .razlog("Još nije plaćeno")
                .build();

        when(placanjeRepository.findById(5L)).thenReturn(Optional.of(placanje));

        assertThatThrownBy(() -> refundacijaService.request(5L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("završena plaćanja");
    }

    @Test
    void approve_rejectsWhenApproverIsSameAsRequester() {
        Refundacija refundacija = Refundacija.builder()
                .refundacijaId(9L)
                .status(RefundacijaStatus.TRAZENA)
                .kreiraoId(7L)
                .build();
        Zaposleni zaposleni = new Zaposleni();
        zaposleni.setKorisnikId(7L);

        when(refundacijaRepository.findById(9L)).thenReturn(Optional.of(refundacija));
        when(currentUserService.getCurrentZaposleni()).thenReturn(zaposleni);

        assertThatThrownBy(() -> refundacijaService.approve(9L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Odobravalac");
    }

    @Test
    void execute_rejectsRefundThatIsNotApproved() {
        Refundacija refundacija = Refundacija.builder()
                .refundacijaId(9L)
                .status(RefundacijaStatus.TRAZENA)
                .build();

        when(refundacijaRepository.findById(9L)).thenReturn(Optional.of(refundacija));

        assertThatThrownBy(() -> refundacijaService.execute(9L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("odobrena");
    }

    @Test
    void request_rejectsRefundAboveRemainingAmount() {
        Placanje placanje = Placanje.builder()
                .placanjeId(5L)
                .status(PlacanjeStatus.COMPLETED)
                .iznos(new BigDecimal("100.00"))
                .build();
        RefundacijaDto dto = RefundacijaDto.builder()
                .iznos(new BigDecimal("30.00"))
                .razlog("Prevelik povraćaj")
                .build();

        when(placanjeRepository.findById(5L)).thenReturn(Optional.of(placanje));
        when(refundacijaRepository.sumByPlacanjeIdAndStatuses(
                5L,
                EnumSet.of(RefundacijaStatus.TRAZENA, RefundacijaStatus.ODOBRENA, RefundacijaStatus.IZVRSENA)
        )).thenReturn(new BigDecimal("75.00"));

        assertThatThrownBy(() -> refundacijaService.request(5L, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("refundabilni iznos");
    }

    @Test
    void execute_incomingInvoiceReducesNetExpenseAndRecomputesBudgetItems() {
        Faktura faktura = faktura(TipFakture.ULAZNA);
        faktura.getStavke().add(stavka(101L, new BigDecimal("600.00"), 11L));
        faktura.getStavke().add(stavka(102L, new BigDecimal("400.00"), 12L));
        Placanje placanje = Placanje.builder()
                .placanjeId(5L)
                .faktura(faktura)
                .status(PlacanjeStatus.COMPLETED)
                .iznos(new BigDecimal("1000.00"))
                .build();
        Refundacija refundacija = Refundacija.builder()
                .refundacijaId(9L)
                .placanje(placanje)
                .status(RefundacijaStatus.ODOBRENA)
                .iznos(new BigDecimal("250.00"))
                .build();
        Trosak prvi = trosak(11L, 10L, 20L, "600.00");
        Trosak drugi = trosak(12L, 10L, 30L, "400.00");
        when(refundacijaRepository.findById(9L)).thenReturn(Optional.of(refundacija));
        when(trosakRepository.findById(11L)).thenReturn(Optional.of(prvi));
        when(trosakRepository.findById(12L)).thenReturn(Optional.of(drugi));

        refundacijaService.execute(9L);

        assertThat(refundacija.getStatus()).isEqualTo(RefundacijaStatus.IZVRSENA);
        assertThat(prvi.getRefundiraniIznos()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(drugi.getRefundiraniIznos()).isEqualByComparingTo(new BigDecimal("100.00"));
        verify(fakturaService).recomputePlaceniIznos(faktura);
        verify(fakturaService).autoUpdateStatus(faktura);
        verify(trosakRepository).saveAll(List.of(prvi, drugi));
        verify(budzetService).recomputeStavku(10L, 20L);
        verify(budzetService).recomputeStavku(10L, 30L);
    }

    @Test
    void execute_outgoingInvoiceDoesNotRecomputeBudgetItems() {
        Faktura faktura = faktura(TipFakture.IZLAZNA);
        Placanje placanje = Placanje.builder()
                .placanjeId(5L)
                .faktura(faktura)
                .status(PlacanjeStatus.COMPLETED)
                .iznos(new BigDecimal("300.00"))
                .build();
        Refundacija refundacija = Refundacija.builder()
                .refundacijaId(9L)
                .placanje(placanje)
                .status(RefundacijaStatus.ODOBRENA)
                .iznos(new BigDecimal("100.00"))
                .build();
        when(refundacijaRepository.findById(9L)).thenReturn(Optional.of(refundacija));

        refundacijaService.execute(9L);

        verify(fakturaService).recomputePlaceniIznos(faktura);
        verify(fakturaService).autoUpdateStatus(faktura);
        verify(trosakRepository, never()).findById(any());
        verify(budzetService, never()).recomputeStavku(any(), any());
    }

    private Faktura faktura(TipFakture tip) {
        return Faktura.builder()
                .fakturaId(1L)
                .brojFakture("F-1")
                .tip(tip)
                .status(FakturaStatus.PLACENA)
                .ukupnaIznos(new BigDecimal("1000.00"))
                .placeniIznos(new BigDecimal("1000.00"))
                .build();
    }

    private StavkaFakture stavka(Long id, BigDecimal ukupnaCena, Long trosakId) {
        return StavkaFakture.builder()
                .stavkaFaktureId(id)
                .ukupnaCena(ukupnaCena)
                .trosakId(trosakId)
                .build();
    }

    private Trosak trosak(Long id, Long budzetId, Long kategorijaId, String iznos) {
        return Trosak.builder()
                .trosakId(id)
                .tip(TipTroska.AUTO_ULAZNA)
                .budzetId(budzetId)
                .kategorijaId(kategorijaId)
                .iznos(new BigDecimal(iznos))
                .refundiraniIznos(BigDecimal.ZERO)
                .build();
    }
}
