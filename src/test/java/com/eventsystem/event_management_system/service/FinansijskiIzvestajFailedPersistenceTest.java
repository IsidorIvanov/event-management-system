package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.GenerateIzvestajRequest;
import com.eventsystem.event_management_system.model.FinansijskiIzvestaj;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.repository.FinansijskiIzvestajRepository;
import com.eventsystem.event_management_system.repository.ZaposleniRepository;
import com.eventsystem.event_management_system.utils.enums.FormatIzvestaja;
import com.eventsystem.event_management_system.utils.enums.IzvestajStatus;
import com.eventsystem.event_management_system.utils.enums.StatusKorisnika;
import com.eventsystem.event_management_system.utils.enums.TipIzvestaja;
import com.eventsystem.event_management_system.utils.enums.UlogaZaposlenog;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FinansijskiIzvestajFailedPersistenceTest {

    @Autowired
    private FinansijskiIzvestajRepository finansijskiIzvestajRepository;

    @Autowired
    private FinansijskiIzvestajStatusTx statusTx;

    @Autowired
    private ZaposleniRepository zaposleniRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedRecordPersistsInDatabaseAfterMarkFailed() {
        Zaposleni user = new Zaposleni();
        user.setIme("Test");
        user.setPrezime("Persist");
        user.setEmail("failed-" + UUID.randomUUID() + "@test.com");
        user.setLozinkaHash("hash");
        user.setUloga(UlogaZaposlenog.FINANSIJSKI_KONTROLOR);
        user.setStatus(StatusKorisnika.AKTIVAN);
        user = zaposleniRepository.saveAndFlush(user);

        GenerateIzvestajRequest request = GenerateIzvestajRequest.builder()
                .tip(TipIzvestaja.PO_PERIODU)
                .format(FormatIzvestaja.PDF)
                .periodOd(LocalDate.of(2026, 1, 1))
                .periodDo(LocalDate.of(2026, 1, 31))
                .build();

        Long id = statusTx.createPending(request, user);
        statusTx.markFailed(id, "Simulirana greška generisanja");

        entityManager.clear();

        FinansijskiIzvestaj persisted = finansijskiIzvestajRepository.findById(id).orElseThrow();

        assertThat(persisted.getStatus()).isEqualTo(IzvestajStatus.FAILED);
        assertThat(persisted.getGreskaPoruka()).contains("Simulirana greška");
    }
}
