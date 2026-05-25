package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.RefundacijaDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.Placanje;
import com.eventsystem.event_management_system.repository.PlacanjeRepository;
import com.eventsystem.event_management_system.repository.RefundacijaRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.PlacanjeStatus;
import com.eventsystem.event_management_system.utils.enums.RefundacijaStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
