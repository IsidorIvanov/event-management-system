package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.GenerateIzvestajRequest;
import com.eventsystem.event_management_system.model.FinansijskiIzvestaj;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.repository.FinansijskiIzvestajRepository;
import com.eventsystem.event_management_system.utils.enums.IzvestajStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FinansijskiIzvestajStatusTx {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final FinansijskiIzvestajRepository finansijskiIzvestajRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createPending(GenerateIzvestajRequest request, Zaposleni kreirao) {
        FinansijskiIzvestaj izvestaj = FinansijskiIzvestaj.builder()
                .tip(request.getTip())
                .format(request.getFormat())
                .status(IzvestajStatus.PENDING)
                .dogadjajId(request.getDogadjajId())
                .klijentId(request.getKlijentId())
                .dobavljacId(request.getDobavljacId())
                .periodOd(request.getPeriodOd())
                .periodDo(request.getPeriodDo())
                .kreirao(kreirao)
                .kreiranAt(LocalDateTime.now())
                .build();
        return finansijskiIzvestajRepository.save(izvestaj).getIzvestajId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long izvestajId, String greskaPoruka) {
        FinansijskiIzvestaj izvestaj = finansijskiIzvestajRepository.findById(izvestajId)
                .orElseThrow();
        izvestaj.setStatus(IzvestajStatus.FAILED);
        izvestaj.setGreskaPoruka(truncate(greskaPoruka));
        finansijskiIzvestajRepository.save(izvestaj);
    }

    private String truncate(String message) {
        if (message == null) {
            return "Nepoznata greška";
        }
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
