package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.OpremaDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.Oprema;
import com.eventsystem.event_management_system.repository.CenovnikRepository;
import com.eventsystem.event_management_system.repository.OpremaRepository;
import com.eventsystem.event_management_system.utils.enums.StatusOpreme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpremaServiceTest {

    @Mock
    private OpremaRepository opremaRepository;

    @Mock
    private CenovnikRepository cenovnikRepository;

    @InjectMocks
    private OpremaService opremaService;

    @Test
    void create_setsInitialQuantities() {
        OpremaDto dto = OpremaDto.builder()
                .naziv("Zvučnici PA")
                .kategorija("AUDIO")
                .kolicinaUkupno(5)
                .kolicinaDostupno(5)
                .build();
        when(opremaRepository.save(any(Oprema.class))).thenAnswer(inv -> inv.getArgument(0));

        OpremaDto created = opremaService.create(dto);

        assertThat(created.getKolicinaUkupno()).isEqualTo(5);
        assertThat(created.getStatus()).isEqualTo(StatusOpreme.DOSTUPNO);
    }

    @Test
    void rezervisi_failsWhenInsufficientStock() {
        Oprema oprema = Oprema.builder()
                .opremaId(1L)
                .naziv("LED rasveta")
                .kategorija("RASVETA")
                .kolicinaUkupno(2)
                .kolicinaDostupno(1)
                .kolicinaRezervisano(0)
                .kolicinaNaDogadjaju(1)
                .kolicinaUServisu(0)
                .status(StatusOpreme.NA_DOGADJAJU)
                .build();

        assertThatThrownBy(() -> opremaService.rezervisi(oprema, 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Nedovoljna");
    }

    @Test
    void findOrCreateByNaziv_createsWhenMissing() {
        when(opremaRepository.findAll()).thenReturn(List.of());
        when(opremaRepository.save(any(Oprema.class))).thenAnswer(inv -> {
            Oprema o = inv.getArgument(0);
            o.setOpremaId(10L);
            return o;
        });

        Oprema created = opremaService.findOrCreateByNaziv("Mikrofoni bežični", null);

        assertThat(created.getNaziv()).isEqualTo("Mikrofoni bežični");
        assertThat(created.getKategorija()).isEqualTo("AUDIO");
    }
}
