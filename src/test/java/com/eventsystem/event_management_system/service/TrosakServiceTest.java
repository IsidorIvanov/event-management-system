package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.TrosakDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.Budzet;
import com.eventsystem.event_management_system.model.BudzetKategorija;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.StavkaBudzeta;
import com.eventsystem.event_management_system.model.Trosak;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.model.compositePK.StavkaBudzetaId;
import com.eventsystem.event_management_system.repository.StavkaBudzetaRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.BudzetStatus;
import com.eventsystem.event_management_system.utils.enums.TipTroska;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrosakServiceTest {

    @Mock
    private TrosakRepository trosakRepository;

    @Mock
    private StavkaBudzetaRepository stavkaBudzetaRepository;

    @Mock
    private BudzetService budzetService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TrosakService trosakService;

    @Test
    void createManual_rejectsAutoIncomingCostThroughPublicApi() {
        TrosakDto dto = validDto();
        dto.setTip(TipTroska.AUTO_ULAZNA);

        assertThatThrownBy(() -> trosakService.createManual(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("AUTO_ULAZNA");
    }

    @Test
    void createManual_requiresActiveBudget() {
        TrosakDto dto = validDto();
        StavkaBudzeta stavka = stavka(BudzetStatus.DRAFT, 7L);
        when(stavkaBudzetaRepository.findById(new StavkaBudzetaId(1L, 2L))).thenReturn(Optional.of(stavka));

        assertThatThrownBy(() -> trosakService.createManual(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void createManual_rejectsMismatchedEvent() {
        TrosakDto dto = validDto();
        StavkaBudzeta stavka = stavka(BudzetStatus.ACTIVE, 99L);
        when(stavkaBudzetaRepository.findById(new StavkaBudzetaId(1L, 2L))).thenReturn(Optional.of(stavka));

        assertThatThrownBy(() -> trosakService.createManual(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Događaj");
    }

    @Test
    void createManual_savesManualCostAndRecomputesBudgetItem() {
        TrosakDto dto = validDto();
        StavkaBudzeta stavka = stavka(BudzetStatus.ACTIVE, 7L);
        Zaposleni zaposleni = new Zaposleni();
        zaposleni.setKorisnikId(88L);

        when(stavkaBudzetaRepository.findById(new StavkaBudzetaId(1L, 2L))).thenReturn(Optional.of(stavka));
        when(currentUserService.getCurrentZaposleni()).thenReturn(zaposleni);
        when(trosakRepository.save(any(Trosak.class))).thenAnswer(invocation -> {
            Trosak trosak = invocation.getArgument(0);
            trosak.setTrosakId(10L);
            return trosak;
        });

        TrosakDto saved = trosakService.createManual(dto);

        ArgumentCaptor<Trosak> captor = ArgumentCaptor.forClass(Trosak.class);
        verify(trosakRepository).save(captor.capture());
        verify(budzetService).recomputeStavku(1L, 2L);
        assertThat(captor.getValue().getTip()).isEqualTo(TipTroska.RUCNI);
        assertThat(captor.getValue().getIznos()).isEqualByComparingTo(new BigDecimal("25.50"));
        assertThat(captor.getValue().getEvidentiraoId()).isEqualTo(88L);
        assertThat(saved.getTrosakId()).isEqualTo(10L);
    }

    private TrosakDto validDto() {
        return TrosakDto.builder()
                .budzetId(1L)
                .kategorijaId(2L)
                .dogadjajId(7L)
                .opis("Ručni trošak")
                .iznos(new BigDecimal("25.50"))
                .datumTroska(LocalDate.now())
                .tip(TipTroska.RUCNI)
                .build();
    }

    private StavkaBudzeta stavka(BudzetStatus status, Long dogadjajId) {
        Dogadjaj dogadjaj = Dogadjaj.builder()
                .dogadjajId(dogadjajId)
                .naziv("Konferencija")
                .build();
        Budzet budzet = Budzet.builder()
                .budzetId(1L)
                .dogadjaj(dogadjaj)
                .status(status)
                .build();
        return StavkaBudzeta.builder()
                .id(new StavkaBudzetaId(1L, 2L))
                .budzet(budzet)
                .kategorija(BudzetKategorija.builder().kategorijaId(2L).naziv("Catering").build())
                .build();
    }
}
