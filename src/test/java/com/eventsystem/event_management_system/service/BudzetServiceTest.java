package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.BudzetAlertDto;
import com.eventsystem.event_management_system.dto.StavkaBudzetaDto;
import com.eventsystem.event_management_system.model.Budzet;
import com.eventsystem.event_management_system.model.BudzetKategorija;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.StavkaBudzeta;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.model.compositePK.StavkaBudzetaId;
import com.eventsystem.event_management_system.utils.enums.AlertLevel;
import com.eventsystem.event_management_system.repository.BudzetRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.StavkaBudzetaRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.BudzetStatus;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
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
import static org.mockito.ArgumentMatchers.eq;
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

    @Test
    void updateStavka_recalculatesAlertWhenThresholdChangesWithoutNewCost() {
        Long budzetId = 1L;
        Long kategorijaId = 2L;
        StavkaBudzetaId id = new StavkaBudzetaId(budzetId, kategorijaId);
        Dogadjaj dogadjaj = Dogadjaj.builder()
                .dogadjajId(10L)
                .naziv("Konferencija")
                .status(StatusDogadjaja.AKTIVAN)
                .build();
        Zaposleni kreirao = new Zaposleni();
        kreirao.setKorisnikId(7L);
        kreirao.setIme("Ana");
        kreirao.setPrezime("Fin");
        Budzet budzet = Budzet.builder()
                .budzetId(budzetId)
                .dogadjaj(dogadjaj)
                .kreirao(kreirao)
                .nazivBudzeta("Budžet")
                .planiraniIznos(new BigDecimal("1000.00"))
                .odobreniIznos(new BigDecimal("1000.00"))
                .status(BudzetStatus.ACTIVE)
                .build();
        StavkaBudzeta stavka = StavkaBudzeta.builder()
                .id(id)
                .budzet(budzet)
                .kategorija(BudzetKategorija.builder().kategorijaId(kategorijaId).naziv("Catering").build())
                .planiraniIznos(new BigDecimal("1000.00"))
                .stvarniIznos(new BigDecimal("850.00"))
                .pragUpozorenja(new BigDecimal("0.7000"))
                .pragKriticnog(new BigDecimal("0.9500"))
                .statusKontrole(StatusKontrole.ISPOD_PLANA)
                .build();
        budzet.getStavke().add(stavka);

        when(budzetRepository.findByIdWithDetalji(budzetId)).thenReturn(Optional.of(budzet));
        when(stavkaBudzetaRepository.findById(id)).thenReturn(Optional.of(stavka));
        when(stavkaBudzetaRepository.sumPlaniraniByBudzetId(budzetId)).thenReturn(new BigDecimal("1000.00"));

        budzetService.updateStavka(
                budzetId,
                kategorijaId,
                StavkaBudzetaDto.builder()
                        .kategorijaId(kategorijaId)
                        .planiraniIznos(new BigDecimal("1000.00"))
                        .pragUpozorenja(new BigDecimal("0.7000"))
                        .pragKriticnog(new BigDecimal("0.8000"))
                        .build()
        );

        BudzetAlertDto result = budzetAlertService.toDto(
                budzetId,
                kategorijaId,
                "Catering",
                new BigDecimal("850.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("0.7000"),
                new BigDecimal("0.8000")
        );
        assertEquals(AlertLevel.CRITICAL, result.getAlertLevel());
        verify(budzetAlertService).evaluateAndRemember(
                eq(budzetId),
                eq(kategorijaId),
                eq("Catering"),
                eq(new BigDecimal("850.00")),
                eq(new BigDecimal("1000.00")),
                eq(new BigDecimal("0.7000")),
                eq(new BigDecimal("0.8000"))
        );
    }
}
