package com.eventsystem.event_management_system.event;

import com.eventsystem.event_management_system.dto.NotifikacijaResponseDto;
import com.eventsystem.event_management_system.service.EmailService;
import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
import com.eventsystem.event_management_system.utils.enums.TipNotifikacije;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Isporučuje notifikaciju primaocu tek nakon što transakcija koja ju je
 * kreirala uspešno commit-uje ({@link TransactionPhase#AFTER_COMMIT}). Radi
 * asinhrono ({@link Async}) da ne blokira poslovnu operaciju.
 *
 * <p>PUSH kanal koristi postojeću WebSocket/STOMP infrastrukturu i šalje na
 * {@code /user/{email}/queue/notifikacije}. Ako je na eventu postavljen
 * {@code emailNaslov}, notifikacija se dodatno isporučuje i mejlom.</p>
 */
@Component
@RequiredArgsConstructor
public class NotifikacijaEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotifikacijaCreated(NotifikacijaCreatedEvent event) {
        if (event.getKanal() == KanalNotifikacije.PUSH) {
            messagingTemplate.convertAndSendToUser(
                    event.getPrimalacEmail(),
                    "/queue/notifikacije",
                    event.getDto()
            );
        }
        // Opciono dodatna isporuka mejlom (npr. D3 — oslobođeno mesto, D5 — izmena).
        if (event.getEmailNaslov() != null) {
            NotifikacijaResponseDto dto = event.getDto();
            // Email može imati zaseban tekst (kraći uvod); ako nije zadat, koristi sadržaj notifikacije.
            String poruka = event.getEmailPoruka() != null ? event.getEmailPoruka() : dto.getSadrzaj();
            emailService.posaljiObavestenje(new EmailService.ObavestenjeEmail(
                    event.getPrimalacEmail(),
                    event.getPrimalacIme(),
                    event.getEmailNaslov(),
                    poruka,
                    tipLabel(dto.getTip()),
                    event.getEmailDetalji()
            ));
        }
    }

    private String tipLabel(TipNotifikacije tip) {
        if (tip == null) return "Obaveštenje";
        return switch (tip) {
            case DOGADJAJ -> "Događaj";
            case SESIJA -> "Sesija";
            case PODSETNIK -> "Podsetnik";
            case OPSTE -> "Opšte";
        };
    }
}
