package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DetektovanaPotrebaDto;
import com.eventsystem.event_management_system.dto.ValidacijaPotrebaDto;
import com.eventsystem.event_management_system.model.Cenovnik;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.repository.CenovnikRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDobavljaca;
import com.eventsystem.event_management_system.utils.enums.TipSesije;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PotrebaOpremeDetekcijaServiceTest {

    @Mock
    private DogadjajRepository dogadjajRepository;

    @Mock
    private SesijaRepository sesijaRepository;

    @Mock
    private CenovnikRepository cenovnikRepository;

    @InjectMocks
    private PotrebaOpremeDetekcijaService detekcijaService;

    @Test
    void detektujPotrebe_ukljucujeAudioIZaVelikiDogadjajLed() {
        Dogadjaj dogadjaj = Dogadjaj.builder()
                .dogadjajId(1L)
                .naziv("Tech Summit")
                .maksKapacitet(600)
                .build();

        when(dogadjajRepository.findByIdWithLokacija(1L)).thenReturn(Optional.of(dogadjaj));
        when(sesijaRepository.findAllByDogadjaj_DogadjajId(1L)).thenReturn(List.of(
                sesija(TipSesije.KEYNOTE),
                sesija(TipSesije.WORKSHOP),
                sesija(TipSesije.WORKSHOP)
        ));
        when(cenovnikRepository.findSveDostupne()).thenReturn(List.of(
                cenovnik("Zvučnici PA"),
                cenovnik("LED rasveta"),
                cenovnik("Bina modul"),
                cenovnik("Mikrofoni bežični")
        ));

        List<DetektovanaPotrebaDto> potrebe = detekcijaService.detektujPotrebe(1L);

        assertThat(potrebe).extracting(DetektovanaPotrebaDto::getNazivResursa)
                .contains("Zvučnici PA", "LED rasveta", "Bina modul", "Mikrofoni bežični");
    }

    @Test
    void validirajPotrebe_100PostoTacnostKadJeSveUCenovniku() {
        Dogadjaj dogadjaj = Dogadjaj.builder().dogadjajId(1L).naziv("Meetup").maksKapacitet(50).build();
        when(dogadjajRepository.findByIdWithLokacija(1L)).thenReturn(Optional.of(dogadjaj));
        when(dogadjajRepository.findById(1L)).thenReturn(Optional.of(dogadjaj));
        when(sesijaRepository.findAllByDogadjaj_DogadjajId(1L)).thenReturn(List.of());
        when(cenovnikRepository.findSveDostupne()).thenReturn(List.of(cenovnik("Zvučnici PA")));

        ValidacijaPotrebaDto v = detekcijaService.validirajPotrebe(1L);

        assertThat(v.isValidno()).isTrue();
        assertThat(v.getNepokriveneStavke()).isEmpty();
        assertThat(v.getPokrivenePotrebe()).isEqualTo(v.getUkupnoPotreba());
    }

    private Sesija sesija(TipSesije tip) {
        return Sesija.builder().tip(tip).kapacitet(100).build();
    }

    private Cenovnik cenovnik(String naziv) {
        Dobavljac d = new Dobavljac();
        d.setDobavljacId(1L);
        d.setStatus(StatusDobavljaca.AKTIVAN);
        return Cenovnik.builder()
                .cenovnikId(1L)
                .dobavljac(d)
                .nazivResursa(naziv)
                .cenaJedinicna(BigDecimal.TEN)
                .dostupnost(true)
                .build();
    }
}
