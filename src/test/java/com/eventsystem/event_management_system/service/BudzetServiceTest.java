package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.BudzetAlertDto;
import com.eventsystem.event_management_system.model.BudzetKategorija;
import com.eventsystem.event_management_system.model.StavkaBudzeta;
import com.eventsystem.event_management_system.model.compositePK.StavkaBudzetaId;
import com.eventsystem.event_management_system.utils.enums.AlertLevel;
import com.eventsystem.event_management_system.repository.BudzetRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.StavkaBudzetaRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.StatusKontrole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudzetServiceTest {

    @Mock
    private BudzetRepository budzetRepository;

    @Mock
    private StavkaBudzetaRepository stavkaBudzetaRepository;

    @Mock
    private DogadjajRepository dogadjajRepository;

    @Mock
    private BudzetKategorijaService kategorijaService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private TrosakRepository trosakRepository;

    @Mock
    private EntityManager entityManager;

    @Spy
    private BudzetAlertService budzetAlertService = new BudzetAlertService();

    @InjectMocks
    private BudzetService budzetService;

    @Test
    void recomputeStavku_updatesActualAmountAndControlStatusFromCosts() {
        StavkaBudzetaId id = new StavkaBudzetaId(1L, 2L);
        StavkaBudzeta stavka = StavkaBudzeta.builder()
                .id(id)
                .kategorija(BudzetKategorija.builder().kategorijaId(2L).naziv("Catering").build())
                .planiraniIznos(new BigDecimal("100.00"))
                .stvarniIznos(BigDecimal.ZERO)
                .pragUpozorenja(new BigDecimal("0.8000"))
                .pragKriticnog(new BigDecimal("0.9500"))
                .build();

        when(stavkaBudzetaRepository.findById(id)).thenReturn(Optional.of(stavka));
        when(trosakRepository.sumNetoByBudzetAndKategorija(1L, 2L))
                .thenReturn(new BigDecimal("125.00"));

        budzetService.recomputeStavku(1L, 2L);

        ArgumentCaptor<StavkaBudzeta> captor = ArgumentCaptor.forClass(StavkaBudzeta.class);
        verify(stavkaBudzetaRepository).save(captor.capture());
        assertThat(captor.getValue().getStvarniIznos()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(captor.getValue().getStatusKontrole()).isEqualTo(StatusKontrole.PREKORACENJE);
    }

    @Test
    void recomputeStavku_keepsControlStatusSeparateFromAlertLevel() {
        StavkaBudzetaId id = new StavkaBudzetaId(1L, 2L);
        StavkaBudzeta stavka = StavkaBudzeta.builder()
                .id(id)
                .kategorija(BudzetKategorija.builder().kategorijaId(2L).naziv("Catering").build())
                .planiraniIznos(new BigDecimal("100.00"))
                .stvarniIznos(BigDecimal.ZERO)
                .pragUpozorenja(new BigDecimal("0.8000"))
                .pragKriticnog(new BigDecimal("0.9500"))
                .build();

        when(stavkaBudzetaRepository.findById(id)).thenReturn(Optional.of(stavka));
        when(trosakRepository.sumNetoByBudzetAndKategorija(1L, 2L))
                .thenReturn(new BigDecimal("97.00"));

        budzetService.recomputeStavku(1L, 2L);

        ArgumentCaptor<StavkaBudzeta> captor = ArgumentCaptor.forClass(StavkaBudzeta.class);
        verify(stavkaBudzetaRepository).save(captor.capture());
        BudzetAlertDto result = budzetAlertService.toDto(
                1L,
                2L,
                "Catering",
                new BigDecimal("97.00"),
                new BigDecimal("100.00"),
                new BigDecimal("0.8000"),
                new BigDecimal("0.9500")
        );

        // ratio = 0.97
        // status_kontrole: 0.97 < (1 - TOL_SK=0.02) = 0.98 -> ISPOD_PLANA
        // alert_level: 0.97 >= prag_kriticnog=0.95 i < 1.0 -> CRITICAL
        // Ovo nije kontradikcija - status_kontrole koristi simetricni
        // prozor oko 1.0, alert koristi apsolutne pragove. Oba su ispravna.
        assertEquals(StatusKontrole.ISPOD_PLANA, captor.getValue().getStatusKontrole());
        assertEquals(AlertLevel.CRITICAL, result.getAlertLevel());
    }
}
