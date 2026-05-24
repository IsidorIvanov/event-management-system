package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.KontaktDobavljacaDto;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.model.Nabavka;
import com.eventsystem.event_management_system.model.StavkaNabavke;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DobavljacKontaktService {

    private static final Logger log = LoggerFactory.getLogger(DobavljacKontaktService.class);

    private final NabavkaService nabavkaService;
    private final DobavljacService dobavljacService;

    @Transactional(readOnly = true)
    public KontaktDobavljacaDto pripremiEmailUpit(Long nabavkaId) {
        Nabavka nabavka = nabavkaService.findEntityWithDetalji(nabavkaId);
        if (nabavka.getDobavljac() == null) {
            throw new RuntimeException("Nabavka nema dodeljenog dobavljaca. Prvo pokrenite automatsku selekciju.");
        }
        return buildKontakt(nabavka, nabavka.getDobavljac(), false);
    }

    @Transactional
    public KontaktDobavljacaDto posaljiEmailUpit(Long nabavkaId) {
        Nabavka nabavka = nabavkaService.findEntityWithDetalji(nabavkaId);
        Dobavljac dobavljac = nabavka.getDobavljac();
        if (dobavljac == null && nabavkaId != null) {
            throw new RuntimeException("Nabavka nema dodeljenog dobavljaca.");
        }
        KontaktDobavljacaDto kontakt = buildKontakt(nabavka, dobavljac, true);

        log.info("=== SIMULACIJA SLANJA EMAIL-a ===");
        log.info("Primalac: {}", kontakt.getPrimaocEmail());
        log.info("Subject: {}", kontakt.getSubject());
        log.info("Body:\n{}", kontakt.getBody());
        log.info("=================================");

        nabavkaService.updateStatus(nabavkaId, StatusNabavke.POTVRDJENA);

        return kontakt;
    }

    @Transactional
    public KontaktDobavljacaDto kontaktirajDobavljaca(Long nabavkaId, Long dobavljacId) {
        Nabavka nabavka = nabavkaService.findEntityWithDetalji(nabavkaId);
        Dobavljac dobavljac = dobavljacService.findEntity(dobavljacId);
        nabavkaService.assignDobavljac(nabavkaId, dobavljacId);
        return buildKontakt(nabavka, dobavljac, true);
    }

    private KontaktDobavljacaDto buildKontakt(Nabavka nabavka, Dobavljac dobavljac, boolean simulirajSlanje) {
        String subject = String.format(
                "Upit za nabavku — dogadjaj: %s (#%d)",
                nabavka.getDogadjaj().getNaziv(),
                nabavka.getDogadjaj().getDogadjajId()
        );

        String stavkeTekst = nabavka.getStavke().stream()
                .map(this::formatStavka)
                .collect(Collectors.joining("\n"));

        String body = """
                Poštovani %s,

                obracamo Vam se u vezi nabavke resursa za događaj "%s" (%s — %s).

                ID nabavke: %d
                Status: %s
                Ukupna procenjena vrednost: %s RSD

                Stavke nabavke:
                %s

                Molimo Vas da nam dostavite ponudu i rok isporuke.

                Srdačan pozdrav,
                %s %s
                Event Management System
                """.formatted(
                dobavljac.getNaziv(),
                nabavka.getDogadjaj().getNaziv(),
                nabavka.getDogadjaj().getDatumPocetka(),
                nabavka.getDogadjaj().getDatumZavrsetka(),
                nabavka.getNabavkaId(),
                nabavka.getStatus(),
                nabavka.getUkupnaCena(),
                stavkeTekst.isBlank() ? "(stavke još nisu definisane)" : stavkeTekst,
                nabavka.getKreirao().getIme(),
                nabavka.getKreirao().getPrezime()
        );

        return KontaktDobavljacaDto.builder()
                .nabavkaId(nabavka.getNabavkaId())
                .dobavljacId(dobavljac.getDobavljacId())
                .dobavljacNaziv(dobavljac.getNaziv())
                .primaocEmail(dobavljac.getKontaktEmail())
                .subject(subject)
                .body(body)
                .poslato(simulirajSlanje)
                .vremePripreme(LocalDateTime.now())
                .napomena(simulirajSlanje
                        ? "Email je simuliran (log). Za produkciju povezati SMTP servis."
                        : "Pregled poruke pre slanja.")
                .build();
    }

    private String formatStavka(StavkaNabavke s) {
        return String.format(
                "- %s | kolicina: %d | jed. cena: %s | ukupno: %s",
                s.getNazivResursa(),
                s.getKolicina(),
                s.getJedinicnaCena(),
                s.getUkupnaCena()
        );
    }
}
