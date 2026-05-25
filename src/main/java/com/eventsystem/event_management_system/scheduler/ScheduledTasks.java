package com.eventsystem.event_management_system.scheduler;

import com.eventsystem.event_management_system.repository.FakturaRepository;
import com.eventsystem.event_management_system.utils.enums.FakturaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final FakturaRepository fakturaRepository;

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
}
