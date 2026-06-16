package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DetektovanaPotrebaDto;
import com.eventsystem.event_management_system.model.Oprema;
import com.eventsystem.event_management_system.repository.OpremaRepository;
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
    private OpremaRepository opremaRepository;

    @InjectMocks
    private InventarPotrebeService inventarPotrebeService;

    @Test
    void validirajPotrebe_reportsShortage() {
        when(potrebaOpremeDetekcijaService.detektujPotrebe(1L)).thenReturn(List.of(
                DetektovanaPotrebaDto.builder().nazivResursa("Zvučnici PA").kolicina(3).build()
        ));
        when(opremaService.dostupnaKolicinaZaNaziv("Zvučnici PA")).thenReturn(1);

        var result = inventarPotrebeService.validirajPotrebeInventarom(1L);

        assertThat(result.isSvePokriveno()).isFalse();
        assertThat(result.getUpozorenja()).isNotEmpty();
        assertThat(result.getStavke().get(0).isPokriveno()).isFalse();
    }
}
