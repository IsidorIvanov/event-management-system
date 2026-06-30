package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.FinansijskiIzvestajDto;
import com.eventsystem.event_management_system.dto.IzvestajPodaciDto;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.FinansijskiIzvestaj;
import com.eventsystem.event_management_system.repository.FinansijskiIzvestajRepository;
import com.eventsystem.event_management_system.service.izvestaj.ExcelIzvestajGenerator;
import com.eventsystem.event_management_system.service.izvestaj.PdfIzvestajGenerator;
import com.eventsystem.event_management_system.utils.enums.FormatIzvestaja;
import com.eventsystem.event_management_system.utils.enums.IzvestajStatus;
import com.eventsystem.event_management_system.utils.enums.TipIzvestaja;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FinansijskiIzvestajGenerateTx {

    private final FinansijskiIzvestajRepository finansijskiIzvestajRepository;
    private final FinansijskaAgregacijaService finansijskaAgregacijaService;
    private final DogadjajIzvestajService dogadjajIzvestajService;
    private final LocalFileStorageService fileStorageService;
    private final PdfIzvestajGenerator pdfIzvestajGenerator;
    private final ExcelIzvestajGenerator excelIzvestajGenerator;

    @Transactional
    public FinansijskiIzvestaj run(Long izvestajId) {
        FinansijskiIzvestaj izvestaj = finansijskiIzvestajRepository.findById(izvestajId)
                .orElseThrow(() -> new NotFoundException("Finansijski izveštaj nije pronađen sa id: " + izvestajId));

        izvestaj.setStatus(IzvestajStatus.GENERATING);
        finansijskiIzvestajRepository.save(izvestaj);

        IzvestajPodaciDto podaci = buildPodaci(izvestaj);
        byte[] content = generateFile(podaci, izvestaj.getFormat());

        String extension = izvestaj.getFormat() == FormatIzvestaja.PDF ? "pdf" : "xlsx";
        LocalDateTime now = LocalDateTime.now();
        String relativePath = String.format("izvestaji/%d/%02d/izvestaj-%d.%s",
                now.getYear(), now.getMonthValue(), izvestajId, extension);

        String fileUrl = fileStorageService.save(content, relativePath);
        izvestaj.setFileUrl(fileUrl);
        izvestaj.setStatus(IzvestajStatus.READY);
        izvestaj.setGenerisanoAt(now);
        izvestaj.setGreskaPoruka(null);

        return finansijskiIzvestajRepository.save(izvestaj);
    }

    private IzvestajPodaciDto buildPodaci(FinansijskiIzvestaj izvestaj) {
        return switch (izvestaj.getTip()) {
            case PO_DOGADJAJU -> finansijskaAgregacijaService.agregacijaPoDogadjaju(izvestaj.getDogadjajId());
            case RESURSI_I_TROSKOVI_PO_DOGADJAJU -> dogadjajIzvestajService.toIzvestajPodaci(
                    dogadjajIzvestajService.getKompletanIzvestaj(izvestaj.getDogadjajId()));
            case PO_KLIJENTU -> finansijskaAgregacijaService.agregacijaPoKlijentu(
                    izvestaj.getKlijentId(), izvestaj.getPeriodOd(), izvestaj.getPeriodDo());
            case PO_DOBAVLJACU -> finansijskaAgregacijaService.agregacijaPoDobavljacu(
                    izvestaj.getDobavljacId(), izvestaj.getPeriodOd(), izvestaj.getPeriodDo());
            case PO_PERIODU -> finansijskaAgregacijaService.agregacijaPoPeriodu(
                    izvestaj.getPeriodOd(), izvestaj.getPeriodDo());
        };
    }

    private byte[] generateFile(IzvestajPodaciDto podaci, FormatIzvestaja format) {
        return switch (format) {
            case PDF -> pdfIzvestajGenerator.generate(podaci, format);
            case EXCEL -> excelIzvestajGenerator.generate(podaci, format);
        };
    }
}
