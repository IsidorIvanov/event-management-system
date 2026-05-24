package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.SalaDostupnostResponseDto;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.model.Sala;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.model.compositePK.SalaId;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import com.eventsystem.event_management_system.repository.SalaRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import com.eventsystem.event_management_system.utils.enums.TipSale;
import com.eventsystem.event_management_system.utils.enums.TipSesije;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaDostupnostServiceTest {

    @Mock
    private LokacijaRepository lokacijaRepository;

    @Mock
    private SalaRepository salaRepository;

    @Mock
    private SesijaRepository sesijaRepository;

    @InjectMocks
    private SalaDostupnostService salaDostupnostService;

    @Test
    void getDostupnost_marksOccupiedHourlySlots() {
        LocalDate datum = LocalDate.of(2026, 5, 24);
        SalaId salaId = new SalaId(1L, "Sala A");
        Lokacija lokacija = Lokacija.builder().lokacijaId(1L).build();
        Sala sala = Sala.builder()
                .id(salaId)
                .lokacija(lokacija)
                .tipSale(TipSale.GLAVNA)
                .kapacitet(100)
                .baznaCenaPoDanu(new BigDecimal("1000"))
                .build();

        Sesija sesija = Sesija.builder()
                .sesijaId(10L)
                .sala(sala)
                .naziv("Panel diskusija")
                .datum(datum)
                .vremePocetka(LocalTime.of(10, 0))
                .vremeZavrsetka(LocalTime.of(12, 0))
                .tip(TipSesije.PANEL)
                .kapacitet(50)
                .dogadjaj(Dogadjaj.builder().dogadjajId(1L).build())
                .build();

        when(lokacijaRepository.existsById(1L)).thenReturn(true);
        when(salaRepository.findByLokacija_LokacijaId(1L)).thenReturn(List.of(sala));
        when(sesijaRepository.findBySala_Lokacija_LokacijaIdAndDatumBetween(1L, datum, datum))
                .thenReturn(List.of(sesija));

        SalaDostupnostResponseDto result = salaDostupnostService.getDostupnost(1L, datum, datum);

        assertThat(result.getSale()).hasSize(1);
        var slotovi = result.getSale().get(0).getDani().get(0).getSlotovi();
        assertThat(slotovi).hasSize(12);

        assertThat(slotovi.get(1).isDostupno()).isTrue();
        assertThat(slotovi.get(2).isDostupno()).isFalse();
        assertThat(slotovi.get(3).isDostupno()).isFalse();
        assertThat(slotovi.get(4).isDostupno()).isTrue();
    }

    @Test
    void getDostupnost_throwsWhenDateRangeInvalid() {
        when(lokacijaRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> salaDostupnostService.getDostupnost(
                1L,
                LocalDate.of(2026, 5, 30),
                LocalDate.of(2026, 5, 24)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Datum do mora biti");
    }
}
