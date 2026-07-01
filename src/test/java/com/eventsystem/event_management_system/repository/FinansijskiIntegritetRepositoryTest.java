package com.eventsystem.event_management_system.repository;

import com.eventsystem.event_management_system.model.Budzet;
import com.eventsystem.event_management_system.model.BudzetKategorija;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.model.Faktura;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.model.StavkaBudzeta;
import com.eventsystem.event_management_system.model.StavkaFakture;
import com.eventsystem.event_management_system.model.Trosak;
import com.eventsystem.event_management_system.model.Ugovor;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.model.compositePK.StavkaBudzetaId;
import com.eventsystem.event_management_system.utils.enums.AlertLevel;
import com.eventsystem.event_management_system.utils.enums.BudzetStatus;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.StatusDobavljaca;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import com.eventsystem.event_management_system.utils.enums.StatusKontrole;
import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
import com.eventsystem.event_management_system.utils.enums.TipTroska;
import com.eventsystem.event_management_system.utils.enums.UlogaZaposlenog;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class FinansijskiIntegritetRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UgovorRepository ugovorRepository;

    @Test
    void databaseRejectsTrosakWithoutExistingBudgetItem() {
        Trosak orphan = Trosak.builder()
                .budzetId(-1000L)
                .kategorijaId(-2000L)
                .dogadjajId(-3000L)
                .opis("Siroče trošak")
                .iznos(new BigDecimal("10.00"))
                .refundiraniIznos(BigDecimal.ZERO)
                .datumTroska(LocalDate.now())
                .tip(TipTroska.RUCNI)
                .build();

        assertThatThrownBy(() -> {
            entityManager.persist(orphan);
            entityManager.flush();
        }).satisfies(this::assertForeignKeyViolation);
    }

    @Test
    void databaseRejectsStavkaFaktureWithoutExistingBudgetItem() {
        Faktura faktura = Faktura.builder()
                .brojFakture("TEST-FK-ORPHAN")
                .tip(TipFakture.ULAZNA)
                .status(FakturaStatus.DRAFT)
                .ukupnaIznos(BigDecimal.ZERO)
                .placeniIznos(BigDecimal.ZERO)
                .build();
        entityManager.persist(faktura);
        entityManager.flush();

        StavkaFakture orphan = StavkaFakture.builder()
                .faktura(faktura)
                .redniBroj(1)
                .naziv("Siroče stavka")
                .kolicina(BigDecimal.ONE)
                .jedinicnaCena(new BigDecimal("10.00"))
                .ukupnaCena(new BigDecimal("10.00"))
                .budzetId(-1000L)
                .kategorijaId(-2000L)
                .build();

        assertThatThrownBy(() -> {
            entityManager.persist(orphan);
            entityManager.flush();
        }).satisfies(this::assertForeignKeyViolation);
    }

    @Test
    void databaseRejectsTrosakWhenRefundExceedsAmount() {
        StavkaBudzeta stavka = persistValidBudgetItem();
        Trosak invalid = Trosak.builder()
                .budzetId(stavka.getId().getBudzetId())
                .kategorijaId(stavka.getId().getKategorijaId())
                .dogadjajId(stavka.getBudzet().getDogadjaj().getDogadjajId())
                .opis("Refund veći od troška")
                .iznos(new BigDecimal("10.00"))
                .refundiraniIznos(new BigDecimal("999.00"))
                .datumTroska(LocalDate.now())
                .tip(TipTroska.RUCNI)
                .build();

        assertThatThrownBy(() -> {
            entityManager.persist(invalid);
            entityManager.flush();
        }).satisfies(this::assertCheckViolation);
    }

    @Test
    void findExpiredActiveReturnsOnlyActiveContractsPastExpiryDate() {
        String suffix = String.valueOf(System.nanoTime());
        Dobavljac dobavljac = persistValidDobavljac(suffix);
        Dogadjaj dogadjaj = persistValidDogadjaj(suffix);

        Ugovor activeExpired = persistUgovor(
                suffix + "-ACTIVE-EXPIRED",
                dobavljac,
                dogadjaj,
                StatusUgovora.AKTIVAN,
                LocalDate.now().minusDays(1)
        );
        Ugovor activeFuture = persistUgovor(
                suffix + "-ACTIVE-FUTURE",
                dobavljac,
                dogadjaj,
                StatusUgovora.AKTIVAN,
                LocalDate.now().plusDays(1)
        );
        Ugovor alreadyExpired = persistUgovor(
                suffix + "-ISTEKAO",
                dobavljac,
                dogadjaj,
                StatusUgovora.ISTEKAO,
                LocalDate.now().minusDays(1)
        );
        entityManager.flush();
        entityManager.clear();

        var result = ugovorRepository.findExpiredActive(LocalDate.now());

        assertThat(result)
                .extracting(Ugovor::getUgovorId)
                .contains(activeExpired.getUgovorId())
                .doesNotContain(activeFuture.getUgovorId(), alreadyExpired.getUgovorId());
    }

    @Test
    void databaseRejectsFakturaWithNonExistingContract() {
        Faktura invalid = Faktura.builder()
                .brojFakture("TEST-UGOVOR-FK-" + System.nanoTime())
                .tip(TipFakture.ULAZNA)
                .status(FakturaStatus.DRAFT)
                .ukupnaIznos(BigDecimal.ZERO)
                .placeniIznos(BigDecimal.ZERO)
                .ugovorId(-999999L)
                .build();

        assertThatThrownBy(() -> {
            entityManager.persist(invalid);
            entityManager.flush();
        }).satisfies(this::assertForeignKeyViolation);
    }

    private void assertForeignKeyViolation(Throwable throwable) {
        Throwable rootCause = rootCause(throwable);
        assertThat(rootCause)
                .isInstanceOf(SQLIntegrityConstraintViolationException.class);
        assertThat(rootCause.getMessage().toLowerCase()).contains("foreign key");
    }

    private void assertCheckViolation(Throwable throwable) {
        Throwable rootCause = rootCause(throwable);
        assertThat(rootCause)
                .isInstanceOf(SQLException.class);
        String message = rootCause.getMessage().toLowerCase();
        assertThat(message).contains("check constraint");
        assertThat(message).contains("trosak_chk_refund_bounds");
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private StavkaBudzeta persistValidBudgetItem() {
        String suffix = String.valueOf(System.nanoTime());
        Lokacija lokacija = Lokacija.builder()
                .naziv("FK test lokacija " + suffix)
                .adresa("Test adresa")
                .grad("Novi Sad")
                .drzava("Srbija")
                .build();
        entityManager.persist(lokacija);

        Dogadjaj dogadjaj = Dogadjaj.builder()
                .lokacija(lokacija)
                .naziv("FK test događaj " + suffix)
                .datumPocetka(LocalDate.now())
                .datumZavrsetka(LocalDate.now().plusDays(1))
                .maksKapacitet(100)
                .status(StatusDogadjaja.OBJAVLJEN)
                .build();
        entityManager.persist(dogadjaj);

        Zaposleni zaposleni = new Zaposleni();
        zaposleni.setIme("Test");
        zaposleni.setPrezime("Finansije");
        zaposleni.setEmail("fk-test-" + suffix + "@example.com");
        zaposleni.setLozinkaHash("test");
        zaposleni.setUloga(UlogaZaposlenog.FINANSIJSKI_KONTROLOR);
        entityManager.persist(zaposleni);

        Budzet budzet = Budzet.builder()
                .dogadjaj(dogadjaj)
                .kreirao(zaposleni)
                .nazivBudzeta("FK test budžet " + suffix)
                .planiraniIznos(new BigDecimal("100.00"))
                .odobreniIznos(new BigDecimal("100.00"))
                .status(BudzetStatus.ACTIVE)
                .build();
        entityManager.persist(budzet);

        BudzetKategorija kategorija = BudzetKategorija.builder()
                .naziv("FK test kategorija " + suffix)
                .opis("Test")
                .build();
        entityManager.persist(kategorija);
        entityManager.flush();

        StavkaBudzeta stavka = StavkaBudzeta.builder()
                .id(new StavkaBudzetaId(budzet.getBudzetId(), kategorija.getKategorijaId()))
                .budzet(budzet)
                .kategorija(kategorija)
                .planiraniIznos(new BigDecimal("100.00"))
                .stvarniIznos(BigDecimal.ZERO)
                .pragUpozorenja(new BigDecimal("0.8000"))
                .pragKriticnog(new BigDecimal("0.9500"))
                .statusKontrole(StatusKontrole.NA_PLANU)
                .lastAlertLevel(AlertLevel.NONE)
                .build();
        entityManager.persist(stavka);
        entityManager.flush();
        return stavka;
    }

    private Dobavljac persistValidDobavljac(String suffix) {
        Dobavljac dobavljac = Dobavljac.builder()
                .naziv("F4 test dobavljač " + suffix)
                .grad("Novi Sad")
                .kontaktEmail("f4-dobavljac-" + suffix + "@example.com")
                .telefon("060123456")
                .pib("PIB" + suffix.substring(Math.max(0, suffix.length() - 9)))
                .status(StatusDobavljaca.AKTIVAN)
                .rejting(new BigDecimal("4.50"))
                .build();
        entityManager.persist(dobavljac);
        return dobavljac;
    }

    private Dogadjaj persistValidDogadjaj(String suffix) {
        Lokacija lokacija = Lokacija.builder()
                .naziv("F4 test lokacija " + suffix)
                .adresa("Test adresa")
                .grad("Novi Sad")
                .drzava("Srbija")
                .build();
        entityManager.persist(lokacija);

        Dogadjaj dogadjaj = Dogadjaj.builder()
                .lokacija(lokacija)
                .naziv("F4 test događaj " + suffix)
                .datumPocetka(LocalDate.now())
                .datumZavrsetka(LocalDate.now().plusDays(1))
                .maksKapacitet(100)
                .status(StatusDogadjaja.OBJAVLJEN)
                .build();
        entityManager.persist(dogadjaj);
        return dogadjaj;
    }

    private Ugovor persistUgovor(
            String brojUgovora,
            Dobavljac dobavljac,
            Dogadjaj dogadjaj,
            StatusUgovora status,
            LocalDate vaziDo
    ) {
        Ugovor ugovor = Ugovor.builder()
                .brojUgovora(brojUgovora)
                .dobavljac(dobavljac)
                .dogadjaj(dogadjaj)
                .datumPotpisivanja(LocalDate.now().minusDays(30))
                .vaziDo(vaziDo)
                .vrednost(new BigDecimal("100.00"))
                .predmet("F4 test ugovor")
                .status(status)
                .build();
        entityManager.persist(ugovor);
        return ugovor;
    }
}
