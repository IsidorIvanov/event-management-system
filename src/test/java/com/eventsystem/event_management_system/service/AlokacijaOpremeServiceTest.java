package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DetektovanaPotrebaDto;
import com.eventsystem.event_management_system.dto.DodelaOpremeDto;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Oprema;
import com.eventsystem.event_management_system.repository.DodelaOpremeRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.OpremaRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlokacijaOpremeServiceTest {

    @Mock
    private PotrebaOpremeDetekcijaService potrebaOpremeDetekcijaService;
    @Mock
    private DogadjajRepository dogadjajRepository;
    @Mock
    private SesijaRepository sesijaRepository;
    @Mock
    private OpremaRepository opremaRepository;
    @Mock
    private DodelaOpremeRepository dodelaOpremeRepository;
    @Mock
    private DodelaOpremeService dodelaOpremeService;

    @InjectMocks
    private AlokacijaOpremeService alokacijaOpremeService;

    @Test
    void predlogZaDogadjaj_dodeljujeIzInventara() {
        Dogadjaj dogadjaj = Dogadjaj.builder()
                .dogadjajId(1L)
                .naziv("Konferencija")
                .datumPocetka(LocalDate.now().plusDays(5))
                .datumZavrsetka(LocalDate.now().plusDays(6))
                .build();
        Oprema oprema = Oprema.builder()
                .opremaId(10L)
                .naziv("Zvučnici PA")
                .kategorija("AUDIO")
                .kolicinaDostupno(4)
                .build();

        when(dogadjajRepository.findById(1L)).thenReturn(Optional.of(dogadjaj));
        when(potrebaOpremeDetekcijaService.detektujPotrebe(1L)).thenReturn(List.of(
                DetektovanaPotrebaDto.builder().nazivResursa("Zvučnici PA").kolicina(2).build()
        ));
        when(sesijaRepository.findAllByDogadjaj_DogadjajId(1L)).thenReturn(List.of());
        when(dodelaOpremeRepository.findByDogadjajWithDetalji(1L)).thenReturn(List.of());
        when(opremaRepository.findAll()).thenReturn(List.of(oprema));

        var plan = alokacijaOpremeService.predlogZaDogadjaj(1L);

        assertThat(plan.isSvePokriveno()).isTrue();
        assertThat(plan.getStavke()).hasSize(1);
        assertThat(plan.getStavke().getFirst().getPredlozenaKolicina()).isEqualTo(2);
        assertThat(plan.getStavke().getFirst().getOpremaId()).isEqualTo(10L);
    }

    @Test
    void primeni_kreiraRezervacije() {
        Dogadjaj dogadjaj = Dogadjaj.builder()
                .dogadjajId(1L)
                .naziv("Konferencija")
                .datumPocetka(LocalDate.now().plusDays(5))
                .datumZavrsetka(LocalDate.now().plusDays(6))
                .build();
        Oprema oprema = Oprema.builder()
                .opremaId(10L)
                .naziv("Zvučnici PA")
                .kategorija("AUDIO")
                .kolicinaDostupno(2)
                .build();

        when(dogadjajRepository.findById(1L)).thenReturn(Optional.of(dogadjaj));
        when(potrebaOpremeDetekcijaService.detektujPotrebe(1L)).thenReturn(List.of(
                DetektovanaPotrebaDto.builder().nazivResursa("Zvučnici PA").kolicina(2).build()
        ));
        when(sesijaRepository.findAllByDogadjaj_DogadjajId(1L)).thenReturn(List.of());
        when(dodelaOpremeRepository.findByDogadjajWithDetalji(1L)).thenReturn(List.of());
        when(opremaRepository.findAll()).thenReturn(List.of(oprema));
        when(dodelaOpremeService.rezervisi(any())).thenReturn(
                DodelaOpremeDto.builder().dodelaId(99L).opremaId(10L).kolicina(2).build()
        );

        var res = alokacijaOpremeService.primeni(1L);

        assertThat(res.getKreiraneDodele()).hasSize(1);
        verify(dodelaOpremeService).rezervisi(any());
    }
}
