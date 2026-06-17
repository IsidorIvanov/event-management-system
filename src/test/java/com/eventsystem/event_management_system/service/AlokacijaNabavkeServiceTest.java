package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.PlanAlokacijeDto;
import com.eventsystem.event_management_system.dto.PotrebnaStavkaDto;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlokacijaNabavkeServiceTest {

    @Mock
    private CenovnikRepository cenovnikRepository;

    @Mock
    private NabavkaService nabavkaService;

    @Mock
    private com.eventsystem.event_management_system.repository.NabavkaRepository nabavkaRepository;

    @Mock
    private DobavljacService dobavljacService;

    @InjectMocks
    private AlokacijaNabavkeService alokacijaService;

    @Test
    void optimizuj_biraNajjeftinijegPoStavci() {
        Dobavljac a = dobavljac(1L, "A");
        Dobavljac b = dobavljac(2L, "B");

        when(cenovnikRepository.findSveDostupne()).thenReturn(List.of(
                cenovnik(1L, a, "Zvucnici", BigDecimal.valueOf(10000)),
                cenovnik(2L, b, "Zvucnici", BigDecimal.valueOf(12000)),
                cenovnik(3L, b, "LED", BigDecimal.valueOf(5000)),
                cenovnik(4L, a, "LED", BigDecimal.valueOf(8000))
        ));

        PlanAlokacijeDto plan = alokacijaService.optimizuj(List.of(
                PotrebnaStavkaDto.builder().nazivResursa("Zvucnici").kolicina(2).build(),
                PotrebnaStavkaDto.builder().nazivResursa("LED").kolicina(1).build()
        ));

        assertThat(plan.isSvePokriveno()).isTrue();
        assertThat(plan.getBrojDobavljaca()).isEqualTo(2);
        assertThat(plan.getUkupnaCena()).isEqualByComparingTo(BigDecimal.valueOf(25000));
        assertThat(plan.getStavke().get(0).getDobavljacId()).isEqualTo(1L);
        assertThat(plan.getStavke().get(1).getDobavljacId()).isEqualTo(2L);
    }

    @Test
    void optimizuj_upozorenjeKadNemaPonude() {
        when(cenovnikRepository.findSveDostupne()).thenReturn(List.of());

        PlanAlokacijeDto plan = alokacijaService.optimizuj(List.of(
                PotrebnaStavkaDto.builder().nazivResursa("Bina").kolicina(1).build()
        ));

        assertThat(plan.isSvePokriveno()).isFalse();
        assertThat(plan.getUpozorenja()).isNotEmpty();
    }

    private Dobavljac dobavljac(Long id, String naziv) {
        Dobavljac d = new Dobavljac();
        d.setDobavljacId(id);
        d.setNaziv(naziv);
        d.setStatus(StatusDobavljaca.AKTIVAN);
        return d;
    }

    private Cenovnik cenovnik(Long id, Dobavljac d, String resurs, BigDecimal cena) {
        return Cenovnik.builder()
                .cenovnikId(id)
                .dobavljac(d)
                .nazivResursa(resurs)
                .cenaJedinicna(cena)
                .dostupnost(true)
                .build();
    }
}
