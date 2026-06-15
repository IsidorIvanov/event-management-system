package com.eventsystem.event_management_system.event;

import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
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
 * {@code /user/{email}/queue/notifikacije}. EMAIL kanal je predviđen za
 * buduću integraciju.</p>
 */
@Component
@RequiredArgsConstructor
public class NotifikacijaEventListener {

    private final SimpMessagingTemplate messagingTemplate;

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
        // EMAIL kanal: buduća integracija sa EmailService za trajne notifikacije.
    }
}
