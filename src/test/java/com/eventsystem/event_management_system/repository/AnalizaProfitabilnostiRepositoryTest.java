package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Govornik;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.model.Sala;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.model.compositePK.SalaId;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import com.eventsystem.event_management_system.utils.enums.TipSale;
import com.eventsystem.event_management_system.utils.enums.TipSesije;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AnalizaProfitabilnostiRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private GovornikRepository govornikRepository;

    @Test
    void sumHonorarByDogadjaj_doesNotDuplicateSpeakerAcrossMultipleSessions() {
        String suffix = String.valueOf(System.nanoTime());
        Lokacija lokacija = persistLokacija(suffix);
        Sala sala = persistSala(lokacija);
        Dogadjaj dogadjaj = persistDogadjaj(lokacija, suffix);
        Govornik govornik = persistGovornik(suffix, new BigDecimal("500.00"));

        persistSesija(dogadjaj, sala, govornik, "Keynote " + suffix, LocalTime.of(9, 0));
        persistSesija(dogadjaj, sala, govornik, "Panel " + suffix, LocalTime.of(11, 0));
        entityManager.flush();
        entityManager.clear();

        BigDecimal result = govornikRepository.sumHonorarByDogadjaj(dogadjaj.getDogadjajId());

        assertThat(result).isEqualByComparingTo("500.00");
    }

    private Lokacija persistLokacija(String suffix) {
        Lokacija lokacija = Lokacija.builder()
                .naziv("Analiza lokacija " + suffix)
                .adresa("Test adresa")
                .grad("Novi Sad")
                .drzava("Srbija")
                .build();
        entityManager.persist(lokacija);
        entityManager.flush();
        return lokacija;
    }

    private Sala persistSala(Lokacija lokacija) {
        Sala sala = Sala.builder()
                .id(new SalaId(lokacija.getLokacijaId(), "Analiza sala"))
                .lokacija(lokacija)
                .tipSale(TipSale.GLAVNA)
                .kapacitet(100)
                .baznaCenaPoDanu(new BigDecimal("100.00"))
                .build();
        entityManager.persist(sala);
        return sala;
    }

    private Dogadjaj persistDogadjaj(Lokacija lokacija, String suffix) {
        Dogadjaj dogadjaj = Dogadjaj.builder()
                .lokacija(lokacija)
                .naziv("Analiza događaj " + suffix)
                .datumPocetka(LocalDate.now().minusDays(2))
                .datumZavrsetka(LocalDate.now().minusDays(1))
                .maksKapacitet(100)
                .status(StatusDogadjaja.ZAVRSEN)
                .build();
        entityManager.persist(dogadjaj);
        return dogadjaj;
    }

    private Govornik persistGovornik(String suffix, BigDecimal honorar) {
        Govornik govornik = Govornik.builder()
                .ime("Govornik")
                .prezime("Analiza")
                .email("analiza-govornik-" + suffix + "@example.com")
                .honorar(honorar)
                .build();
        entityManager.persist(govornik);
        return govornik;
    }

    private void persistSesija(
            Dogadjaj dogadjaj,
            Sala sala,
            Govornik govornik,
            String naziv,
            LocalTime vremePocetka
    ) {
        Sesija sesija = Sesija.builder()
                .dogadjaj(dogadjaj)
                .sala(sala)
                .naziv(naziv)
                .datum(dogadjaj.getDatumPocetka())
                .vremePocetka(vremePocetka)
                .vremeZavrsetka(vremePocetka.plusHours(1))
                .tip(TipSesije.KEYNOTE)
                .kapacitet(100)
                .build();
        sesija.getGovornici().add(govornik);
        entityManager.persist(sesija);
    }
}
