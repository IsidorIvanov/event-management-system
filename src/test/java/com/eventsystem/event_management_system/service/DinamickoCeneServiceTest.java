package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.PredlogCeneKarteDto;
import com.eventsystem.event_management_system.model.Cenovnik;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.TipKarte;
import com.eventsystem.event_management_system.model.compositePK.TipKarteId;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.SalaRepository;
import com.eventsystem.event_management_system.repository.TipKarteRepository;
import com.eventsystem.event_management_system.utils.enums.VrstaKarte;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DinamickoCeneServiceTest {

    @Mock
    private DogadjajRepository dogadjajRepository;
    @Mock
    private TipKarteRepository tipKarteRepository;
    @Mock
    private SalaRepository salaRepository;
    @Mock
    private PotraznjaMetrikeService potraznjaMetrikeService;

    @InjectMocks
    private DinamickoCeneService dinamickoCeneService;

    @Test
    void predlogCenaKarata_visokaPotraznjaPovecavaCenu() {
        Dogadjaj d = Dogadjaj.builder()
                .dogadjajId(1L)
                .datumPocetka(LocalDate.now().plusDays(10))
                .build();
        TipKarte tip = TipKarte.builder()
                .id(new TipKarteId(1L, "Standard"))
                .dogadjaj(d)
                .vrsta(VrstaKarte.VISEDNEVNA)
                .cena(BigDecimal.valueOf(1000))
                .kvota(100)
                .build();

        when(dogadjajRepository.findById(1L)).thenReturn(java.util.Optional.of(d));
        when(tipKarteRepository.findAllByDogadjaj_DogadjajId(1L)).thenReturn(List.of(tip));
        when(potraznjaMetrikeService.danaDoDogadjaja(1L)).thenReturn(10L);
        when(potraznjaMetrikeService.ukupnaKvotaDogadjaja(1L)).thenReturn(100);
        when(potraznjaMetrikeService.ukupnoProdatoDogadjaja(1L)).thenReturn(85);
        when(potraznjaMetrikeService.prodatoKarata(1L, "Standard")).thenReturn(85);

        PredlogCeneKarteDto predlog = dinamickoCeneService.predlogCenaKarata(1L).getTipoviKarata().getFirst();

        assertThat(predlog.getPredlozenaCena()).isEqualByComparingTo(BigDecimal.valueOf(1100));
        assertThat(predlog.getPravilo()).contains("+10%");
    }

    @Test
    void predlogCenaKarata_niskaProdajaSmanjujeCenu() {
        Dogadjaj d = Dogadjaj.builder()
                .dogadjajId(2L)
                .datumPocetka(LocalDate.now().plusDays(5))
                .build();
        TipKarte tip = TipKarte.builder()
                .id(new TipKarteId(2L, "VIP"))
                .dogadjaj(d)
                .vrsta(VrstaKarte.VISEDNEVNA)
                .cena(BigDecimal.valueOf(2000))
                .kvota(50)
                .build();

        when(dogadjajRepository.findById(2L)).thenReturn(java.util.Optional.of(d));
        when(tipKarteRepository.findAllByDogadjaj_DogadjajId(2L)).thenReturn(List.of(tip));
        when(potraznjaMetrikeService.danaDoDogadjaja(2L)).thenReturn(5L);
        when(potraznjaMetrikeService.ukupnaKvotaDogadjaja(2L)).thenReturn(50);
        when(potraznjaMetrikeService.ukupnoProdatoDogadjaja(2L)).thenReturn(10);
        when(potraznjaMetrikeService.prodatoKarata(2L, "VIP")).thenReturn(10);

        PredlogCeneKarteDto predlog = dinamickoCeneService.predlogCenaKarata(2L).getTipoviKarata().getFirst();

        assertThat(predlog.getPredlozenaCena()).isEqualByComparingTo(BigDecimal.valueOf(1700));
    }

    @Test
    void predlogCeneCenovnika_visokaPotraznjaPovecavaCenu() {
        com.eventsystem.event_management_system.model.Dobavljac d = new com.eventsystem.event_management_system.model.Dobavljac();
        d.setDobavljacId(1L);
        d.setNaziv("Audio Pro");
        Cenovnik c = Cenovnik.builder()
                .cenovnikId(10L)
                .dobavljac(d)
                .nazivResursa("Zvučnici PA")
                .cenaJedinicna(BigDecimal.valueOf(10000))
                .build();

        when(potraznjaMetrikeService.ukupnaPotraznjaKolicina(10L, "Zvučnici PA")).thenReturn(20);
        when(potraznjaMetrikeService.brojDogadjajaSaPotrebom(10L, "Zvučnici PA")).thenReturn(3);
        when(potraznjaMetrikeService.potraznjaIndeksProcenat(10L, "Zvučnici PA", 20)).thenReturn(100);

        var predlog = dinamickoCeneService.predlogCeneCenovnika(c, 20);

        assertThat(predlog.getPredlozenaCena()).isEqualByComparingTo(BigDecimal.valueOf(11000));
        assertThat(predlog.getPravilo()).contains("+10%");
    }

    @Test
    void predlogCeneCenovnika_niskaPotraznjaSmanjujeCenu() {
        com.eventsystem.event_management_system.model.Dobavljac d = new com.eventsystem.event_management_system.model.Dobavljac();
        d.setDobavljacId(2L);
        d.setNaziv("LED Rent");
        Cenovnik c = Cenovnik.builder()
                .cenovnikId(11L)
                .dobavljac(d)
                .nazivResursa("LED panel")
                .cenaJedinicna(BigDecimal.valueOf(5000))
                .build();

        when(potraznjaMetrikeService.ukupnaPotraznjaKolicina(11L, "LED panel")).thenReturn(0);
        when(potraznjaMetrikeService.brojDogadjajaSaPotrebom(11L, "LED panel")).thenReturn(0);
        when(potraznjaMetrikeService.potraznjaIndeksProcenat(11L, "LED panel", 20)).thenReturn(0);

        var predlog = dinamickoCeneService.predlogCeneCenovnika(c, 20);

        assertThat(predlog.getPredlozenaCena()).isEqualByComparingTo(BigDecimal.valueOf(4750));
        assertThat(predlog.getPravilo()).contains("-5%");
    }
}
