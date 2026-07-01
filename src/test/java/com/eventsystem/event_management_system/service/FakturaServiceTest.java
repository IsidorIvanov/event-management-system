package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.FakturaDto;
import com.eventsystem.event_management_system.dto.StavkaFaktureDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.Budzet;
import com.eventsystem.event_management_system.model.BudzetKategorija;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.model.StavkaBudzeta;
import com.eventsystem.event_management_system.model.StavkaFakture;
import com.eventsystem.event_management_system.model.Trosak;
import com.eventsystem.event_management_system.model.compositePK.StavkaBudzetaId;
import com.eventsystem.event_management_system.repository.FakturaRepository;
import com.eventsystem.event_management_system.repository.PlacanjeRepository;
import com.eventsystem.event_management_system.repository.RefundacijaRepository;
import com.eventsystem.event_management_system.repository.StavkaBudzetaRepository;
import com.eventsystem.event_management_system.repository.StavkaFaktureRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.BudzetStatus;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
import com.eventsystem.event_management_system.utils.enums.TipTroska;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FakturaServiceTest {

    @Mock
    private FakturaRepository fakturaRepository;

    @Mock
    private PlacanjeRepository placanjeRepository;

    @Mock
    private RefundacijaRepository refundacijaRepository;

    @Mock
    private StavkaFaktureRepository stavkaFaktureRepository;

    @Mock
    private StavkaBudzetaRepository stavkaBudzetaRepository;

    @Mock
    private TrosakRepository trosakRepository;

    @Mock
    private BudzetService budzetService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private FakturaService fakturaService;

    @Test
    void createIncoming_rejectsClientOnIncomingInvoice() {
        FakturaDto dto = FakturaDto.builder()
                .brojFakture("UL-1")
                .tip(TipFakture.ULAZNA)
                .dogadjajId(100L)
                .dobavljacId(5L)
                .klijentId(6L)
                .datumIzdavanja(LocalDate.now())
                .rokPlacanja(LocalDate.now().plusDays(7))
                .build();

        when(fakturaRepository.findByBrojFakture("UL-1")).thenReturn(null);

        assertThatThrownBy(() -> fakturaService.createIncoming(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ne sme imati klijenta");
    }

    @Test
    void createOutgoing_rejectsSupplierOnOutgoingInvoice() {
        FakturaDto dto = FakturaDto.builder()
                .brojFakture("IZ-1")
                .tip(TipFakture.IZLAZNA)
                .dogadjajId(100L)
                .klijentId(6L)
                .dobavljacId(5L)
                .datumIzdavanja(LocalDate.now())
                .rokPlacanja(LocalDate.now().plusDays(7))
                .build();

        when(fakturaRepository.findByBrojFakture("IZ-1")).thenReturn(null);

        assertThatThrownBy(() -> fakturaService.createOutgoing(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ne sme imati dobavljača");
    }

    @Test
    void addStavka_ulaznaFakturaNeCreatesTrosak() {
        Faktura faktura = faktura(TipFakture.ULAZNA);
        StavkaBudzeta stavkaBudzeta = stavkaBudzeta();
        when(fakturaRepository.findById(1L)).thenReturn(Optional.of(faktura));
        when(stavkaBudzetaRepository.findById(new StavkaBudzetaId(10L, 20L)))
                .thenReturn(Optional.of(stavkaBudzeta));
        when(stavkaFaktureRepository.maxRedniBrojByFakturaId(1L)).thenReturn(0);
        when(stavkaFaktureRepository.save(any(StavkaFakture.class))).thenAnswer(invocation -> {
            StavkaFakture saved = invocation.getArgument(0);
            if (saved.getStavkaFaktureId() == null) {
                saved.setStavkaFaktureId(55L);
            }
            return saved;
        });
        when(stavkaFaktureRepository.sumUkupnaCenaByFakturaId(1L)).thenReturn(new BigDecimal("150.00"));

        fakturaService.addStavka(1L, stavkaDto());

        verify(trosakRepository, never()).save(any(Trosak.class));
        verify(budzetService, never()).recomputeStavku(any(), any());
    }

    @Test
    void addStavka_izlaznaFakturaDoesNotCreateExpense() {
        Faktura faktura = faktura(TipFakture.IZLAZNA);
        StavkaBudzeta stavkaBudzeta = stavkaBudzeta();
        when(fakturaRepository.findById(1L)).thenReturn(Optional.of(faktura));
        when(stavkaBudzetaRepository.findById(new StavkaBudzetaId(10L, 20L)))
                .thenReturn(Optional.of(stavkaBudzeta));
        when(stavkaFaktureRepository.maxRedniBrojByFakturaId(1L)).thenReturn(0);
        when(stavkaFaktureRepository.save(any(StavkaFakture.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stavkaFaktureRepository.sumUkupnaCenaByFakturaId(1L)).thenReturn(new BigDecimal("150.00"));

        fakturaService.addStavka(1L, stavkaDto());

        verify(trosakRepository, never()).save(any(Trosak.class));
        verify(budzetService, never()).recomputeStavku(10L, 20L);
    }

    @Test
    void deleteStavka_ulaznaFakturaDraftNeKompenzujeTrosak() {
        Faktura faktura = faktura(TipFakture.ULAZNA);
        StavkaFakture stavka = StavkaFakture.builder()
                .stavkaFaktureId(55L)
                .faktura(faktura)
                .budzetId(10L)
                .kategorijaId(20L)
                .trosakId(null)
                .build();
        when(fakturaRepository.findById(1L)).thenReturn(Optional.of(faktura));
        when(stavkaFaktureRepository.findById(55L)).thenReturn(Optional.of(stavka));
        when(stavkaFaktureRepository.sumUkupnaCenaByFakturaId(1L)).thenReturn(BigDecimal.ZERO);

        fakturaService.deleteStavka(1L, 55L);

        verify(trosakRepository, never()).save(any(Trosak.class));
        verify(budzetService).recomputeStavku(10L, 20L);
    }

    @Test
    void cancelFaktura_ulaznaFakturaCompensatesAutoExpensesAndRecomputesUniquePairs() {
        Faktura faktura = faktura(TipFakture.ULAZNA);
        Trosak prvi = autoTrosak(77L, 10L, 20L, "100.00");
        Trosak drugi = autoTrosak(78L, 10L, 20L, "50.00");
        when(fakturaRepository.findById(1L)).thenReturn(Optional.of(faktura));
        when(trosakRepository.findByFakturaIdAndTip(1L, TipTroska.AUTO_ULAZNA))
                .thenReturn(List.of(prvi, drugi));

        fakturaService.cancelFaktura(1L);

        assertThat(prvi.getRefundiraniIznos()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(drugi.getRefundiraniIznos()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(faktura.getStatus()).isEqualTo(FakturaStatus.OTKAZANA);
        verify(trosakRepository).saveAll(List.of(prvi, drugi));
        verify(budzetService).recomputeStavku(10L, 20L);
    }

    @Test
    void issue_ulaznaFakturaCreatesAutoExpenseForEachStavkaAndRecomputes() {
        Faktura faktura = faktura(TipFakture.ULAZNA);
        StavkaFakture prva = stavka(55L, faktura, 10L, 20L, "Ozvučenje", "150.00");
        StavkaFakture druga = stavka(56L, faktura, 10L, 30L, "Catering", "250.00");
        faktura.getStavke().addAll(List.of(prva, druga));
        when(fakturaRepository.findById(1L)).thenReturn(Optional.of(faktura));
        when(stavkaFaktureRepository.countByFakturaId(1L)).thenReturn(2L);
        when(stavkaFaktureRepository.sumUkupnaCenaByFakturaId(1L)).thenReturn(new BigDecimal("400.00"));
        when(stavkaFaktureRepository.findByFakturaFakturaId(1L)).thenReturn(List.of(prva, druga));
        when(trosakRepository.save(any(Trosak.class))).thenAnswer(invocation -> {
            Trosak saved = invocation.getArgument(0);
            saved.setTrosakId(saved.getKategorijaId().equals(20L) ? 77L : 78L);
            return saved;
        });

        fakturaService.issue(1L);

        ArgumentCaptor<Trosak> trosakCaptor = ArgumentCaptor.forClass(Trosak.class);
        verify(trosakRepository, times(2)).save(trosakCaptor.capture());
        List<Trosak> troskovi = trosakCaptor.getAllValues();
        assertThat(troskovi).allSatisfy(trosak -> assertThat(trosak.getTip()).isEqualTo(TipTroska.AUTO_ULAZNA));
        assertThat(troskovi).extracting(Trosak::getIznos)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("150.00"), new BigDecimal("250.00"));
        assertThat(prva.getTrosakId()).isEqualTo(77L);
        assertThat(druga.getTrosakId()).isEqualTo(78L);
        assertThat(faktura.getStatus()).isEqualTo(FakturaStatus.IZDATA);
        verify(stavkaFaktureRepository).save(prva);
        verify(stavkaFaktureRepository).save(druga);
        verify(budzetService).recomputeStavku(10L, 20L);
        verify(budzetService).recomputeStavku(10L, 30L);
    }

    @Test
    void issue_rejectsInvoiceWithoutItems() {
        Faktura faktura = faktura(TipFakture.ULAZNA);
        when(fakturaRepository.findById(1L)).thenReturn(Optional.of(faktura));
        when(stavkaFaktureRepository.countByFakturaId(1L)).thenReturn(0L);

        assertThatThrownBy(() -> fakturaService.issue(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("bar jednu stavku");
    }

    @Test
    void issue_izlaznaFakturaDoesNotCreateTrosak() {
        Faktura faktura = faktura(TipFakture.IZLAZNA);
        faktura.getStavke().add(stavka(55L, faktura, 10L, 20L, "Ulaznica", "150.00"));
        when(fakturaRepository.findById(1L)).thenReturn(Optional.of(faktura));
        when(stavkaFaktureRepository.countByFakturaId(1L)).thenReturn(1L);
        when(stavkaFaktureRepository.sumUkupnaCenaByFakturaId(1L)).thenReturn(new BigDecimal("150.00"));

        fakturaService.issue(1L);

        assertThat(faktura.getStatus()).isEqualTo(FakturaStatus.IZDATA);
        verify(trosakRepository, never()).save(any(Trosak.class));
        verify(budzetService, never()).recomputeStavku(any(), any());
    }

    private Faktura faktura(TipFakture tip) {
        return Faktura.builder()
                .fakturaId(1L)
                .brojFakture("F-1")
                .tip(tip)
                .status(FakturaStatus.DRAFT)
                .ukupnaIznos(BigDecimal.ZERO)
                .placeniIznos(BigDecimal.ZERO)
                .dogadjajId(100L)
                .dobavljacId(tip == TipFakture.ULAZNA ? 5L : null)
                .klijentId(tip == TipFakture.IZLAZNA ? 6L : null)
                .kreiraoId(9L)
                .build();
    }

    private StavkaBudzeta stavkaBudzeta() {
        Budzet budzet = Budzet.builder()
                .budzetId(10L)
                .dogadjaj(Dogadjaj.builder().dogadjajId(100L).build())
                .status(BudzetStatus.ACTIVE)
                .build();
        return StavkaBudzeta.builder()
                .id(new StavkaBudzetaId(10L, 20L))
                .budzet(budzet)
                .kategorija(BudzetKategorija.builder().kategorijaId(20L).build())
                .build();
    }

    private StavkaFaktureDto stavkaDto() {
        return StavkaFaktureDto.builder()
                .naziv("Ozvučenje")
                .kolicina(BigDecimal.ONE)
                .jedinicnaCena(new BigDecimal("150.00"))
                .budzetId(10L)
                .kategorijaId(20L)
                .build();
    }

    private StavkaFakture stavka(
            Long stavkaId,
            Faktura faktura,
            Long budzetId,
            Long kategorijaId,
            String naziv,
            String ukupnaCena
    ) {
        return StavkaFakture.builder()
                .stavkaFaktureId(stavkaId)
                .faktura(faktura)
                .budzetId(budzetId)
                .kategorijaId(kategorijaId)
                .naziv(naziv)
                .ukupnaCena(new BigDecimal(ukupnaCena))
                .build();
    }

    private Trosak autoTrosak(Long trosakId, Long budzetId, Long kategorijaId, String iznos) {
        return Trosak.builder()
                .trosakId(trosakId)
                .tip(TipTroska.AUTO_ULAZNA)
                .fakturaId(1L)
                .budzetId(budzetId)
                .kategorijaId(kategorijaId)
                .iznos(new BigDecimal(iznos))
                .refundiraniIznos(BigDecimal.ZERO)
                .build();
    }
}
