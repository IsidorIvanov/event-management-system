package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DodelaOpremeDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Oprema;
import com.eventsystem.event_management_system.repository.DodelaOpremeRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.InventarKretanjeRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDodeleOpreme;
import com.eventsystem.event_management_system.utils.enums.StatusOpreme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DodelaOpremeServiceTest {

    @Mock
    private DodelaOpremeRepository dodelaOpremeRepository;
    @Mock
    private OpremaService opremaService;
    @Mock
    private DogadjajRepository dogadjajRepository;
    @Mock
    private SesijaRepository sesijaRepository;
    @Mock
    private InventarKretanjeRepository inventarKretanjeRepository;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private DodelaOpremeService dodelaOpremeService;

    @Test
    void rezervisi_rejectsInvalidPeriod() {
        DodelaOpremeDto dto = DodelaOpremeDto.builder()
                .opremaId(1L)
                .dogadjajId(2L)
                .kolicina(1)
                .datumOd(LocalDate.of(2026, 6, 10))
                .datumDo(LocalDate.of(2026, 6, 1))
                .build();

        assertThatThrownBy(() -> dodelaOpremeService.rezervisi(dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void aktiviraj_requiresRezervisanoStatus() {
        var dodela = com.eventsystem.event_management_system.model.DodelaOpreme.builder()
                .dodelaId(5L)
                .oprema(Oprema.builder().opremaId(1L).build())
                .dogadjaj(Dogadjaj.builder().dogadjajId(2L).naziv("Test").build())
                .kolicina(1)
                .status(StatusDodeleOpreme.NA_DOGADJAJU)
                .datumOd(LocalDate.now())
                .datumDo(LocalDate.now().plusDays(1))
                .build();
        when(dodelaOpremeRepository.findById(5L)).thenReturn(Optional.of(dodela));

        assertThatThrownBy(() -> dodelaOpremeService.aktiviraj(5L))
                .isInstanceOf(BadRequestException.class);
    }
}
