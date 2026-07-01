package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.AttachDocumentRequest;
import com.eventsystem.event_management_system.dto.CreateUgovorRequest;
import com.eventsystem.event_management_system.dto.UgovorDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Ugovor;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.UgovorRepository;
import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UgovorServiceTest {

    @Mock
    private UgovorRepository ugovorRepository;

    @Mock
    private DobavljacService dobavljacService;

    @Mock
    private DogadjajRepository dogadjajRepository;

    @InjectMocks
    private UgovorService ugovorService;

    @Test
    void create_successCreatesDraftContract() {
        CreateUgovorRequest request = validCreateRequest();
        when(ugovorRepository.existsByBrojUgovora("UG-2026-001")).thenReturn(false);
        when(dobavljacService.findEntity(10L)).thenReturn(dobavljac());
        when(dogadjajRepository.findById(20L)).thenReturn(Optional.of(dogadjaj()));
        when(ugovorRepository.save(any(Ugovor.class))).thenAnswer(invocation -> {
            Ugovor ugovor = invocation.getArgument(0);
            ugovor.setUgovorId(99L);
            return ugovor;
        });

        UgovorDto dto = ugovorService.create(request);

        assertThat(dto.getUgovorId()).isEqualTo(99L);
        assertThat(dto.getBrojUgovora()).isEqualTo("UG-2026-001");
        assertThat(dto.getStatus()).isEqualTo(StatusUgovora.NACRT);
        assertThat(dto.getVrednost()).isEqualByComparingTo("1200.50");
    }

    @Test
    void create_rejectsDuplicateContractNumber() {
        CreateUgovorRequest request = validCreateRequest();
        when(ugovorRepository.existsByBrojUgovora("UG-2026-001")).thenReturn(true);

        assertThatThrownBy(() -> ugovorService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("već postoji");
    }

    @Test
    void create_rejectsNegativeValue() {
        CreateUgovorRequest request = validCreateRequest();
        request.setVrednost(new BigDecimal("-1.00"));

        assertThatThrownBy(() -> ugovorService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("negativna");
    }

    @Test
    void create_rejectsExpiryNotAfterSigningDate() {
        CreateUgovorRequest request = validCreateRequest();
        request.setVaziDo(request.getDatumPotpisivanja());

        assertThatThrownBy(() -> ugovorService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("posle datuma potpisivanja");
    }

    @Test
    void activate_withoutDocumentSucceeds() {
        Ugovor ugovor = ugovor(StatusUgovora.NACRT, LocalDate.now().plusDays(30), null);
        when(ugovorRepository.findById(1L)).thenReturn(Optional.of(ugovor));
        when(ugovorRepository.save(ugovor)).thenReturn(ugovor);

        UgovorDto dto = ugovorService.activate(1L);

        assertThat(dto.getStatus()).isEqualTo(StatusUgovora.AKTIVAN);
    }

    @Test
    void activate_rejectsExpiredDraftContract() {
        Ugovor ugovor = ugovor(StatusUgovora.NACRT, LocalDate.now().minusDays(1), null);
        when(ugovorRepository.findById(1L)).thenReturn(Optional.of(ugovor));

        assertThatThrownBy(() -> ugovorService.activate(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("istekao");
    }

    @Test
    void terminate_allowedFromDraftAndActive() {
        Ugovor draft = ugovor(StatusUgovora.NACRT, LocalDate.now().plusDays(30), null);
        when(ugovorRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(ugovorRepository.save(draft)).thenReturn(draft);

        UgovorDto draftDto = ugovorService.terminate(1L);

        assertThat(draftDto.getStatus()).isEqualTo(StatusUgovora.RASKINUT);

        Ugovor active = ugovor(StatusUgovora.AKTIVAN, LocalDate.now().plusDays(30), "https://example.com/ugovor.pdf");
        when(ugovorRepository.findById(2L)).thenReturn(Optional.of(active));
        when(ugovorRepository.save(active)).thenReturn(active);

        UgovorDto activeDto = ugovorService.terminate(2L);

        assertThat(activeDto.getStatus()).isEqualTo(StatusUgovora.RASKINUT);
    }

    @Test
    void attachDocument_rejectsTerminatedContract() {
        Ugovor ugovor = ugovor(StatusUgovora.RASKINUT, LocalDate.now().plusDays(30), null);
        when(ugovorRepository.findById(1L)).thenReturn(Optional.of(ugovor));

        assertThatThrownBy(() -> ugovorService.attachDocument(1L, new AttachDocumentRequest("https://example.com/a.pdf")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("NACRT ili AKTIVAN");
    }

    private CreateUgovorRequest validCreateRequest() {
        return CreateUgovorRequest.builder()
                .brojUgovora("UG-2026-001")
                .dobavljacId(10L)
                .dogadjajId(20L)
                .datumPotpisivanja(LocalDate.now())
                .vaziDo(LocalDate.now().plusDays(30))
                .vrednost(new BigDecimal("1200.50"))
                .predmet("Ozvučenje za konferenciju")
                .usloviPlacanja("Plaćanje u roku od 15 dana")
                .build();
    }

    private Ugovor ugovor(StatusUgovora status, LocalDate vaziDo, String dokumentUrl) {
        return Ugovor.builder()
                .ugovorId(1L)
                .brojUgovora("UG-2026-001")
                .dobavljac(dobavljac())
                .dogadjaj(dogadjaj())
                .datumPotpisivanja(LocalDate.now().minusDays(1))
                .vaziDo(vaziDo)
                .vrednost(new BigDecimal("100.00"))
                .predmet("Test ugovor")
                .dokumentUrl(dokumentUrl)
                .status(status)
                .build();
    }

    private Dobavljac dobavljac() {
        return Dobavljac.builder()
                .dobavljacId(10L)
                .naziv("Test dobavljač")
                .build();
    }

    private Dogadjaj dogadjaj() {
        return Dogadjaj.builder()
                .dogadjajId(20L)
                .naziv("Test događaj")
                .build();
    }
}
