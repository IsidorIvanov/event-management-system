package com.eventsystem.event_management_system.integration;

import com.eventsystem.event_management_system.dto.BudzetDto;
import com.eventsystem.event_management_system.dto.FakturaDto;
import com.eventsystem.event_management_system.dto.PlacanjeDto;
import com.eventsystem.event_management_system.dto.RefundacijaDto;
import com.eventsystem.event_management_system.dto.StavkaBudzetaDto;
import com.eventsystem.event_management_system.dto.StavkaFaktureDto;
import com.eventsystem.event_management_system.model.BudzetKategorija;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.model.StavkaBudzeta;
import com.eventsystem.event_management_system.model.Trosak;
import com.eventsystem.event_management_system.model.Ugovor;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.model.compositePK.StavkaBudzetaId;
import com.eventsystem.event_management_system.repository.StavkaBudzetaRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.service.BudzetService;
import com.eventsystem.event_management_system.service.FakturaService;
import com.eventsystem.event_management_system.service.PlacanjeService;
import com.eventsystem.event_management_system.service.RefundacijaService;
import com.eventsystem.event_management_system.utils.enums.AlertLevel;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.MetodPlacanja;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import com.eventsystem.event_management_system.utils.enums.TipFakture;
import com.eventsystem.event_management_system.utils.enums.TipTroska;
import com.eventsystem.event_management_system.utils.enums.UlogaZaposlenog;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FinansijeF1F3FlowTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BudzetService budzetService;

    @Autowired
    private FakturaService fakturaService;

    @Autowired
    private PlacanjeService placanjeService;

    @Autowired
    private RefundacijaService refundacijaService;

    @Autowired
    private TrosakRepository trosakRepository;

    @Autowired
    private StavkaBudzetaRepository stavkaBudzetaRepository;

    @Test
    void fullBudgetInvoicePaymentRefundFlowKeepsNetAmountsAndAlertsConsistent() {
        String suffix = String.valueOf(System.nanoTime());
        Zaposleni finansije = persistZaposleni("finansije-" + suffix + "@example.com", UlogaZaposlenog.FINANSIJSKI_KONTROLOR);
        Zaposleni menadzer = persistZaposleni("menadzer-" + suffix + "@example.com", UlogaZaposlenog.MENADZER_DOGADJAJA);
        Dogadjaj dogadjaj = persistDogadjaj(suffix);
        BudzetKategorija kategorija = persistKategorija(suffix);
        Dobavljac dobavljac = persistDobavljac(suffix);
        Ugovor ugovor = persistAktivanUgovor(dobavljac, dogadjaj, suffix);
        entityManager.flush();

        authenticate(finansije.getEmail(), "ROLE_FINANSIJSKI_KONTROLOR", "ROLE_MENADZER_DOGADJAJA");
        BudzetDto budzet = budzetService.create(BudzetDto.builder()
                .dogadjajId(dogadjaj.getDogadjajId())
                .nazivBudzeta("Integracioni budžet " + suffix)
                .planiraniIznos(new BigDecimal("200.00"))
                .build());
        budzetService.addStavka(budzet.getBudzetId(), StavkaBudzetaDto.builder()
                .kategorijaId(kategorija.getKategorijaId())
                .planiraniIznos(new BigDecimal("200.00"))
                .pragUpozorenja(new BigDecimal("0.8000"))
                .pragKriticnog(new BigDecimal("0.9500"))
                .build());
        budzetService.approve(budzet.getBudzetId());
        budzetService.activate(budzet.getBudzetId());

        FakturaDto faktura = fakturaService.createIncoming(FakturaDto.builder()
                .brojFakture("INT-FIN-" + suffix)
                .tip(TipFakture.ULAZNA)
                .dogadjajId(dogadjaj.getDogadjajId())
                .dobavljacId(dobavljac.getDobavljacId())
                .ugovorId(ugovor.getUgovorId())
                .datumIzdavanja(LocalDate.now())
                .rokPlacanja(LocalDate.now().plusDays(7))
                .build());
        fakturaService.addStavka(faktura.getFakturaId(), StavkaFaktureDto.builder()
                .naziv("Integracioni trošak")
                .kolicina(BigDecimal.ONE)
                .jedinicnaCena(new BigDecimal("200.00"))
                .budzetId(budzet.getBudzetId())
                .kategorijaId(kategorija.getKategorijaId())
                .build());
        FakturaDto issued = fakturaService.issue(faktura.getFakturaId());

        List<Trosak> autoTroskovi = trosakRepository.findByFakturaIdAndTip(faktura.getFakturaId(), TipTroska.AUTO_ULAZNA);
        assertThat(issued.getStatus()).isEqualTo(FakturaStatus.IZDATA);
        assertThat(autoTroskovi).hasSize(1);
        assertThat(autoTroskovi.getFirst().getIznos()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(stavka(budzet.getBudzetId(), kategorija.getKategorijaId()).getLastAlertLevel()).isEqualTo(AlertLevel.CRITICAL);

        PlacanjeDto placanje = placanjeService.create(faktura.getFakturaId(), PlacanjeDto.builder()
                .iznos(new BigDecimal("200.00"))
                .datumPlacanja(LocalDate.now())
                .metod(MetodPlacanja.TRANSFER)
                .build());
        placanjeService.confirm(placanje.getPlacanjeId());

        RefundacijaDto refundacija = refundacijaService.request(placanje.getPlacanjeId(), RefundacijaDto.builder()
                .iznos(new BigDecimal("50.00"))
                .razlog("Delimičan povraćaj")
                .build());
        authenticate(menadzer.getEmail(), "ROLE_MENADZER_DOGADJAJA");
        refundacijaService.approve(refundacija.getRefundacijaId());
        authenticate(finansije.getEmail(), "ROLE_FINANSIJSKI_KONTROLOR");
        refundacijaService.execute(refundacija.getRefundacijaId());
        entityManager.flush();
        entityManager.clear();

        Trosak refreshedTrosak = trosakRepository.findByFakturaIdAndTip(faktura.getFakturaId(), TipTroska.AUTO_ULAZNA)
                .getFirst();
        StavkaBudzeta refreshedStavka = stavka(budzet.getBudzetId(), kategorija.getKategorijaId());
        FakturaDto refreshedFaktura = fakturaService.getById(faktura.getFakturaId());

        assertThat(refreshedFaktura.getPlaceniIznos()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(refreshedTrosak.getRefundiraniIznos()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(refreshedStavka.getStvarniIznos()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(refreshedStavka.getLastAlertLevel()).isEqualTo(AlertLevel.NONE);

        StavkaBudzeta stavka = stavka(budzet.getBudzetId(), kategorija.getKategorijaId());
        assertThat(stavka.getId().getBudzetId()).isEqualTo(budzet.getBudzetId());
        assertThat(stavka.getId().getKategorijaId()).isEqualTo(kategorija.getKategorijaId());
    }

    private StavkaBudzeta stavka(Long budzetId, Long kategorijaId) {
        return stavkaBudzetaRepository.findById(new StavkaBudzetaId(budzetId, kategorijaId)).orElseThrow();
    }

    private void authenticate(String email, String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email,
                "test",
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()
        ));
    }

    private Zaposleni persistZaposleni(String email, UlogaZaposlenog uloga) {
        Zaposleni zaposleni = new Zaposleni();
        zaposleni.setIme("Test");
        zaposleni.setPrezime(uloga.name());
        zaposleni.setEmail(email);
        zaposleni.setLozinkaHash("test");
        zaposleni.setUloga(uloga);
        entityManager.persist(zaposleni);
        return zaposleni;
    }

    private Dogadjaj persistDogadjaj(String suffix) {
        Lokacija lokacija = Lokacija.builder()
                .naziv("Lokacija " + suffix)
                .adresa("Adresa 1")
                .grad("Novi Sad")
                .drzava("Srbija")
                .build();
        entityManager.persist(lokacija);
        Dogadjaj dogadjaj = Dogadjaj.builder()
                .lokacija(lokacija)
                .naziv("Finansije flow " + suffix)
                .datumPocetka(LocalDate.now().plusDays(1))
                .datumZavrsetka(LocalDate.now().plusDays(2))
                .maksKapacitet(100)
                .status(StatusDogadjaja.OBJAVLJEN)
                .build();
        entityManager.persist(dogadjaj);
        return dogadjaj;
    }

    private BudzetKategorija persistKategorija(String suffix) {
        BudzetKategorija kategorija = BudzetKategorija.builder()
                .naziv("Integraciona kategorija " + suffix)
                .opis("Test kategorija")
                .build();
        entityManager.persist(kategorija);
        return kategorija;
    }

    private Dobavljac persistDobavljac(String suffix) {
        Dobavljac dobavljac = Dobavljac.builder()
                .naziv("Dobavljač " + suffix)
                .grad("Novi Sad")
                .kontaktEmail("dobavljac-" + suffix + "@example.com")
                .pib(suffix.substring(Math.max(0, suffix.length() - 9)))
                .build();
        entityManager.persist(dobavljac);
        return dobavljac;
    }

    private Ugovor persistAktivanUgovor(Dobavljac dobavljac, Dogadjaj dogadjaj, String suffix) {
        Ugovor ugovor = Ugovor.builder()
                .brojUgovora("INT-UG-" + suffix)
                .dobavljac(dobavljac)
                .dogadjaj(dogadjaj)
                .datumPotpisivanja(LocalDate.now().minusDays(1))
                .vrednost(new BigDecimal("200.00"))
                .predmet("Integracioni dobavljački ugovor")
                .status(StatusUgovora.AKTIVAN)
                .vaziDo(LocalDate.now().plusDays(30))
                .build();
        entityManager.persist(ugovor);
        return ugovor;
    }
}
