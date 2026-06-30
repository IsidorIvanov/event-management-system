package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.FinansijskiIzvestajDto;
import com.eventsystem.event_management_system.dto.GenerateIzvestajRequest;
import com.eventsystem.event_management_system.dto.IzvestajPodaciDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.ConflictException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.FinansijskiIzvestaj;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.repository.FinansijskiIzvestajRepository;
import com.eventsystem.event_management_system.utils.enums.FormatIzvestaja;
import com.eventsystem.event_management_system.utils.enums.IzvestajStatus;
import com.eventsystem.event_management_system.utils.enums.TipIzvestaja;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinansijskiIzvestajServiceTest {

    @Mock private FinansijskiIzvestajRepository finansijskiIzvestajRepository;
    @Mock private FinansijskiIzvestajStatusTx statusTx;
    @Mock private FinansijskiIzvestajGenerateTx generateTx;
    @Mock private LocalFileStorageService fileStorageService;
    @Mock private CurrentUserService currentUserService;

    private FinansijskiIzvestajService service;

    @BeforeEach
    void setUp() {
        service = new FinansijskiIzvestajService(
                finansijskiIzvestajRepository,
                statusTx,
                generateTx,
                fileStorageService,
                currentUserService
        );
    }

    @Test
    void generate_happyPath_returnsReady() {
        GenerateIzvestajRequest request = periodRequest(TipIzvestaja.PO_PERIODU);
        Zaposleni user = zaposleni(1L);
        FinansijskiIzvestaj ready = izvestaj(99L, IzvestajStatus.READY, user);
        ready.setFileUrl("izvestaji/2026/06/izvestaj-99.pdf");
        ready.setGenerisanoAt(LocalDateTime.now());

        when(currentUserService.getCurrentZaposleni()).thenReturn(user);
        when(statusTx.createPending(request, user)).thenReturn(99L);
        when(generateTx.run(99L)).thenReturn(ready);
        when(finansijskiIzvestajRepository.findByIdWithKreirao(99L)).thenReturn(Optional.of(ready));

        FinansijskiIzvestajDto dto = service.generate(request);

        assertThat(dto.getStatus()).isEqualTo(IzvestajStatus.READY);
        assertThat(dto.getFileUrl()).isNotBlank();
        verify(statusTx, never()).markFailed(anyLong(), anyString());
    }

    @Test
    void generate_onFailure_marksFailedAndReturnsDto() {
        GenerateIzvestajRequest request = periodRequest(TipIzvestaja.PO_PERIODU);
        Zaposleni user = zaposleni(1L);
        FinansijskiIzvestaj failed = izvestaj(99L, IzvestajStatus.FAILED, user);
        failed.setGreskaPoruka("Greška generatora");

        when(currentUserService.getCurrentZaposleni()).thenReturn(user);
        when(statusTx.createPending(request, user)).thenReturn(99L);
        when(generateTx.run(99L)).thenThrow(new IllegalStateException("Greška generatora"));
        when(finansijskiIzvestajRepository.findByIdWithKreirao(99L)).thenReturn(Optional.of(failed));

        FinansijskiIzvestajDto dto = service.generate(request);

        verify(statusTx).markFailed(99L, "Greška generatora");
        assertThat(dto.getStatus()).isEqualTo(IzvestajStatus.FAILED);
        assertThat(dto.getGreskaPoruka()).isEqualTo("Greška generatora");
    }

    @Test
    void generate_resursiTip_rejectsPeriod() {
        GenerateIzvestajRequest request = GenerateIzvestajRequest.builder()
                .tip(TipIzvestaja.RESURSI_I_TROSKOVI_PO_DOGADJAJU)
                .format(FormatIzvestaja.PDF)
                .dogadjajId(10L)
                .periodOd(LocalDate.of(2026, 1, 1))
                .periodDo(LocalDate.of(2026, 1, 31))
                .build();

        assertThatThrownBy(() -> service.generate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("period");
    }

    @Test
    void generate_xorValidation_rejectsMixedParameters() {
        GenerateIzvestajRequest request = GenerateIzvestajRequest.builder()
                .tip(TipIzvestaja.PO_KLIJENTU)
                .format(FormatIzvestaja.PDF)
                .klijentId(5L)
                .dogadjajId(10L)
                .periodOd(LocalDate.of(2026, 1, 1))
                .periodDo(LocalDate.of(2026, 1, 31))
                .build();

        assertThatThrownBy(() -> service.generate(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("dogadjajId");
    }

    @Test
    void download_rejectsWhenNotReady() {
        FinansijskiIzvestaj pending = izvestaj(1L, IzvestajStatus.PENDING, zaposleni(1L));
        when(finansijskiIzvestajRepository.findByIdWithKreirao(1L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.download(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("READY");
    }

    @Test
    void download_throws404WhenFileMissing() {
        FinansijskiIzvestaj ready = izvestaj(1L, IzvestajStatus.READY, zaposleni(1L));
        ready.setFileUrl("missing.pdf");
        when(finansijskiIzvestajRepository.findByIdWithKreirao(1L)).thenReturn(Optional.of(ready));
        when(fileStorageService.loadAsResource("missing.pdf")).thenThrow(new NotFoundException("Fajl izveštaja nije pronađen."));

        assertThatThrownBy(() -> service.download(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void download_returnsResourceWhenReady() {
        FinansijskiIzvestaj ready = izvestaj(1L, IzvestajStatus.READY, zaposleni(1L));
        ready.setFormat(FormatIzvestaja.PDF);
        ready.setFileUrl("izvestaji/2026/06/izvestaj-1.pdf");
        Resource resource = new ByteArrayResource(new byte[]{1, 2, 3});

        when(finansijskiIzvestajRepository.findByIdWithKreirao(1L)).thenReturn(Optional.of(ready));
        when(fileStorageService.loadAsResource("izvestaji/2026/06/izvestaj-1.pdf")).thenReturn(resource);

        var response = service.download(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(resource);
    }

    private GenerateIzvestajRequest periodRequest(TipIzvestaja tip) {
        return GenerateIzvestajRequest.builder()
                .tip(tip)
                .format(FormatIzvestaja.PDF)
                .periodOd(LocalDate.of(2026, 1, 1))
                .periodDo(LocalDate.of(2026, 1, 31))
                .build();
    }

    private Zaposleni zaposleni(Long id) {
        Zaposleni z = new Zaposleni();
        z.setKorisnikId(id);
        z.setIme("Fin");
        z.setPrezime("Kontrolor");
        z.setEmail("fin@test.com");
        z.setLozinkaHash("hash");
        return z;
    }

    private FinansijskiIzvestaj izvestaj(Long id, IzvestajStatus status, Zaposleni kreirao) {
        return FinansijskiIzvestaj.builder()
                .izvestajId(id)
                .tip(TipIzvestaja.PO_PERIODU)
                .format(FormatIzvestaja.PDF)
                .status(status)
                .periodOd(LocalDate.of(2026, 1, 1))
                .periodDo(LocalDate.of(2026, 1, 31))
                .kreirao(kreirao)
                .kreiranAt(LocalDateTime.now())
                .build();
    }
}
