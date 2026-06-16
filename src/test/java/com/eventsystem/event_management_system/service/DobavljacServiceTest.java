package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DobavljacDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.model.Dobavljac;
import com.eventsystem.event_management_system.repository.DobavljacRepository;
import com.eventsystem.event_management_system.repository.NabavkaRepository;
import com.eventsystem.event_management_system.repository.UgovorRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDobavljaca;
import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DobavljacServiceTest {

    @Mock
    private DobavljacRepository dobavljacRepository;

    @Mock
    private UgovorRepository ugovorRepository;

    @Mock
    private NabavkaRepository nabavkaRepository;

    @InjectMocks
    private DobavljacService dobavljacService;

    @Test
    void create_rejectsDuplicatePib() {
        DobavljacDto dto = sampleDto();
        when(dobavljacRepository.existsByPib("123456789")).thenReturn(true);

        assertThatThrownBy(() -> dobavljacService.create(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PIB");
    }

    @Test
    void delete_rejectsWhenActiveContractExists() {
        Dobavljac entity = sampleEntity();
        when(dobavljacRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(ugovorRepository.existsByDobavljacDobavljacIdAndStatus(1L, StatusUgovora.AKTIVAN)).thenReturn(true);

        assertThatThrownBy(() -> dobavljacService.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("aktivan ugovor");
    }

    @Test
    void deactivate_setsNeaktivanStatus() {
        Dobavljac entity = sampleEntity();
        when(dobavljacRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(dobavljacRepository.save(entity)).thenReturn(entity);

        DobavljacDto dto = dobavljacService.deactivate(1L);

        assertThat(dto.getStatus()).isEqualTo(StatusDobavljaca.NEAKTIVAN);
        verify(dobavljacRepository, never()).delete(any());
    }

    @Test
    void create_setsDefaultsWithoutOptionalFields() {
        DobavljacDto dto = DobavljacDto.builder()
                .naziv("Nova firma")
                .grad("Niš")
                .kontaktEmail("info@nova.rs")
                .pib("987654321")
                .build();
        when(dobavljacRepository.existsByPib("987654321")).thenReturn(false);
        when(dobavljacRepository.save(any(Dobavljac.class))).thenAnswer(inv -> inv.getArgument(0));

        DobavljacDto created = dobavljacService.create(dto);

        assertThat(created.getStatus()).isEqualTo(StatusDobavljaca.AKTIVAN);
        assertThat(created.getRejting()).isNull();
        assertThat(created.getKriterijumi()).isNull();
    }

    private DobavljacDto sampleDto() {
        return DobavljacDto.builder()
                .naziv("Audio Pro")
                .grad("Beograd")
                .kontaktEmail("info@audio.rs")
                .pib("123456789")
                .rejting(BigDecimal.valueOf(4.5))
                .build();
    }

    private Dobavljac sampleEntity() {
        return Dobavljac.builder()
                .dobavljacId(1L)
                .naziv("Audio Pro")
                .grad("Beograd")
                .kontaktEmail("info@audio.rs")
                .pib("123456789")
                .status(StatusDobavljaca.AKTIVAN)
                .rejting(BigDecimal.valueOf(4.5))
                .build();
    }
}
