package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.AutomatskaSelekcijaRequestDto;
import com.eventsystem.event_management_system.dto.PotrebnaStavkaDto;
import com.eventsystem.event_management_system.dto.PredlogDobavljacaDto;
import com.eventsystem.event_management_system.model.Cenovnik;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.repository.CenovnikRepository;
import com.eventsystem.event_management_system.utils.enums.KriterijumSelekcijeDobavljaca;
import com.eventsystem.event_management_system.utils.enums.StatusDobavljaca;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DobavljacSelekcijaServiceTest {

    @Mock
    private CenovnikRepository cenovnikRepository;

    @Mock
    private NabavkaService nabavkaService;

    @Mock
    private DobavljacService dobavljacService;

    @InjectMocks
    private DobavljacSelekcijaService selekcijaService;

    @Test
    void predloziDobavljaca_biraNajnizuCenuZaSveStavke() {
        Dobavljac jeftiniji = dobavljac(1L, "Event Rent", "event@test.rs", BigDecimal.valueOf(4.0));
        Dobavljac skupiji = dobavljac(2L, "Audio Pro", "audio@test.rs", BigDecimal.valueOf(5.0));

        when(cenovnikRepository.findSveDostupne()).thenReturn(List.of(
                cenovnik(1L, jeftiniji, "Zvucnici PA", BigDecimal.valueOf(13500)),
                cenovnik(2L, skupiji, "Zvucnici PA", BigDecimal.valueOf(15000)),
                cenovnik(3L, jeftiniji, "Mikrofoni bezicni", BigDecimal.valueOf(7500)),
                cenovnik(4L, skupiji, "Mikrofoni bezicni", BigDecimal.valueOf(8000))
        ));

        AutomatskaSelekcijaRequestDto request = AutomatskaSelekcijaRequestDto.builder()
                .nabavkaId(10L)
                .kriterijum(KriterijumSelekcijeDobavljaca.NAJNIZA_CENA)
                .potrebneStavke(List.of(
                        PotrebnaStavkaDto.builder().nazivResursa("Zvucnici PA").kolicina(2).build(),
                        PotrebnaStavkaDto.builder().nazivResursa("Mikrofoni bezicni").kolicina(1).build()
                ))
                .build();

        PredlogDobavljacaDto predlog = selekcijaService.predloziDobavljaca(request);

        assertThat(predlog.getDobavljacId()).isEqualTo(1L);
        assertThat(predlog.isPokrivaSveStavke()).isTrue();
        assertThat(predlog.getUkupnaProcenjenaCena()).isEqualByComparingTo(BigDecimal.valueOf(34500));
    }

    @Test
    void predloziDobavljaca_biraNajboljiRejting() {
        Dobavljac boljiRejting = dobavljac(2L, "Audio Pro", "audio@test.rs", BigDecimal.valueOf(4.9));
        Dobavljac losijiRejting = dobavljac(1L, "Event Rent", "event@test.rs", BigDecimal.valueOf(4.0));

        when(cenovnikRepository.findSveDostupne()).thenReturn(List.of(
                cenovnik(1L, losijiRejting, "LED rasveta", BigDecimal.valueOf(11000)),
                cenovnik(2L, boljiRejting, "LED rasveta", BigDecimal.valueOf(12000))
        ));

        AutomatskaSelekcijaRequestDto request = AutomatskaSelekcijaRequestDto.builder()
                .nabavkaId(10L)
                .kriterijum(KriterijumSelekcijeDobavljaca.NAJBOLJI_REJTING)
                .potrebneStavke(List.of(
                        PotrebnaStavkaDto.builder().nazivResursa("LED rasveta").kolicina(3).build()
                ))
                .build();

        PredlogDobavljacaDto predlog = selekcijaService.predloziDobavljaca(request);

        assertThat(predlog.getDobavljacId()).isEqualTo(2L);
        assertThat(predlog.getRejting()).isEqualByComparingTo(BigDecimal.valueOf(4.9));
    }

    private Dobavljac dobavljac(Long id, String naziv, String email, BigDecimal rejting) {
        Dobavljac d = new Dobavljac();
        d.setDobavljacId(id);
        d.setNaziv(naziv);
        d.setKontaktEmail(email);
        d.setRejting(rejting);
        d.setStatus(StatusDobavljaca.AKTIVAN);
        return d;
    }

    private Cenovnik cenovnik(Long id, Dobavljac dobavljac, String resurs, BigDecimal cena) {
        return Cenovnik.builder()
                .cenovnikId(id)
                .dobavljac(dobavljac)
                .nazivResursa(resurs)
                .cenaJedinicna(cena)
                .dostupnost(true)
                .build();
    }
}
