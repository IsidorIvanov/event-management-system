package com.eventsystem.event_management_system.scheduler;

import com.eventsystem.event_management_system.repository.FakturaRepository;
import com.eventsystem.event_management_system.repository.UgovorRepository;
import com.eventsystem.event_management_system.service.DogadjajService;
import com.eventsystem.event_management_system.service.SesijaService;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final FakturaRepository fakturaRepository;
    private final UgovorRepository ugovorRepository;
    private final DogadjajService dogadjajService;
    private final SesijaService sesijaService;
    private final com.eventsystem.event_management_system.service.DodelaOpremeService dodelaOpremeService;

    // run daily at 02:00
    @Scheduled(cron = "0 0 2 * * *")
    public void mark_overdue_fakture() {
        var list = fakturaRepository.findOverdue(
                LocalDate.now(),
                List.of(FakturaStatus.IZDATA, FakturaStatus.DELIMICNO_PLACENA)
        );
        for (var f : list) {
            f.setStatus(FakturaStatus.DOSPELA);
        }
        fakturaRepository.saveAll(list);
    }

    // run daily at 01:00
    @Scheduled(cron = "0 0 1 * * *")
    public void markExpiredUgovori() {
        var list = ugovorRepository.findExpiredActive(LocalDate.now());
        for (var ugovor : list) {
            ugovor.setStatus(StatusUgovora.ISTEKAO);
        }
        ugovorRepository.saveAll(list);
        log.info("Scheduled job prebacio {} ugovora u ISTEKAO status.", list.size());
    }

    // D6 — na svakih 15 min prebaci započete događaje u AKTIVAN i obavesti učesnike
    @Scheduled(cron = "0 */1 * * * *")
    public void aktivirajZapoceteDogadjaje() {
        int aktivirano = dogadjajService.aktivirajZapoceteDogadjaje();
        if (aktivirano > 0) {
            log.info("Scheduled job aktivirao {} događaja (OBJAVLJEN -> AKTIVAN).", aktivirano);
        }
    }

    // D7 — prebaci događaje kojima je prošao datum završetka u ZAVRSEN i pošalji zahvalnice
    @Scheduled(cron = "0 */1 * * * *")
    public void zavrsiDogadjaje() {
        int zavrseno = dogadjajService.zavrsiDogadjaje();
        if (zavrseno > 0) {
            log.info("Scheduled job završio {} događaja (-> ZAVRSEN).", zavrseno);
        }
    }

    // P1 — podsetnik dan pre početka događaja (priprema QR). U produkciji dovoljno jednom dnevno.
    @Scheduled(cron = "0 */1 * * * *")
    public void posaljiPodsetnikeZaDogadjaje() {
        int poslato = dogadjajService.posaljiPodsetnikeZaDogadjaje();
        if (poslato > 0) {
            log.info("Scheduled job poslao podsetnik (P1) za {} događaja.", poslato);
        }
    }

    // P2 — podsetnik da sesija počinje uskoro (u narednih ~60 min)
    @Scheduled(cron = "0 */1 * * * *")
    public void posaljiPodsetnikeZaSesije() {
        int poslato = sesijaService.posaljiPodsetnikeZaSesije();
        if (poslato > 0) {
            log.info("Scheduled job poslao podsetnik (P2) za {} sesija.", poslato);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void oslobodiIstekleDodeleOpreme() {
        int count = dodelaOpremeService.oslobodiIstekleDodele();
        if (count > 0) {
            log.info("Scheduled job oslobodio opremu sa {} isteklih dodela.", count);
        }
    }
}
