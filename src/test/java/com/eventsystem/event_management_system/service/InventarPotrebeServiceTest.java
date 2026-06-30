package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DetektovanaPotrebaDto;
import com.eventsystem.event_management_system.model.DodelaOpreme;
import com.eventsystem.event_management_system.model.Oprema;
import com.eventsystem.event_management_system.repository.DodelaOpremeRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDodeleOpreme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarPotrebeServiceTest {

    @Mock
    private PotrebaOpremeDetekcijaService potrebaOpremeDetekcijaService;
    @Mock
    private OpremaService opremaService;
    @Mock
    private DodelaOpremeRepository dodelaOpremeRepository;

    @InjectMocks
    private InventarPotrebeService inventarPotrebeService;

    @Test
    void validirajPotrebe_reportsShortageWhenNoDodela() {
        when(potrebaOpremeDetekcijaService.detektujPotrebe(1L)).thenReturn(List.of(
                DetektovanaPotrebaDto.builder().nazivResursa("Zvučnici PA").kolicina(3).build()
        ));
        when(dodelaOpremeRepository.findByDogadjajWithDetalji(1L)).thenReturn(List.of());
        when(opremaService.dostupnaKolicinaZaNaziv("Zvučnici PA")).thenReturn(1);

        var result = inventarPotrebeService.validirajPotrebeInventarom(1L);

        assertThat(result.isSvePokriveno()).isFalse();
        assertThat(result.getUpozorenja()).isNotEmpty();
        assertThat(result.getStavke().get(0).isPokriveno()).isFalse();
    }

    @Test
    void validirajPotrebe_countsActiveDodelaAsCoverage() {
        when(potrebaOpremeDetekcijaService.detektujPotrebe(1L)).thenReturn(List.of(
                DetektovanaPotrebaDto.builder().nazivResursa("Zvučnici PA").kolicina(3).build()
        ));
        Oprema oprema = Oprema.builder().naziv("Zvučnici PA").build();
        DodelaOpreme dodela = DodelaOpreme.builder()
                .oprema(oprema)
                .kolicina(3)
                .status(StatusDodeleOpreme.REZERVISANO)
                .build();
        when(dodelaOpremeRepository.findByDogadjajWithDetalji(1L)).thenReturn(List.of(dodela));
        when(opremaService.dostupnaKolicinaZaNaziv("Zvučnici PA")).thenReturn(1);

        var result = inventarPotrebeService.validirajPotrebeInventarom(1L);

        assertThat(result.isSvePokriveno()).isTrue();
        assertThat(result.getTacnostProcenat()).isEqualTo(100);
        assertThat(result.getStavke().get(0).isPokriveno()).isTrue();
    }
}
