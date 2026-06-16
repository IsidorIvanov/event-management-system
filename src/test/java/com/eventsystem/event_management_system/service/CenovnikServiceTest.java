package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.CenovnikDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.Cenovnik;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.repository.CenovnikRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDobavljaca;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CenovnikServiceTest {

    @Mock
    private CenovnikRepository cenovnikRepository;

    @Mock
    private DobavljacService dobavljacService;

    @InjectMocks
    private CenovnikService cenovnikService;

    @Test
    void create_rejectsSuspendedSupplier() {
        Dobavljac suspended = Dobavljac.builder()
                .dobavljacId(5L)
                .status(StatusDobavljaca.SUSPENDOVAN)
                .build();
        when(dobavljacService.findEntity(5L)).thenReturn(suspended);

        CenovnikDto dto = CenovnikDto.builder()
                .dobavljacId(5L)
                .nazivResursa("Zvucnici PA")
                .jedinicaMere("kom")
                .cenaJedinicna(BigDecimal.TEN)
                .build();

        assertThatThrownBy(() -> cenovnikService.create(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("suspendovan");
    }
}
