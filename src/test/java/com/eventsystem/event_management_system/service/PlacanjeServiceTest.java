package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.PlacanjeDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.repository.PlacanjeRepository;
import com.eventsystem.event_management_system.repository.UgovorRepository;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.MetodPlacanja;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

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
}
