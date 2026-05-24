package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.UgovorDto;
import com.eventsystem.event_management_system.model.Ugovor;
import com.eventsystem.event_management_system.repository.UgovorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UgovorService {

    private final UgovorRepository ugovorRepository;
    private final DobavljacService dobavljacService;
    private final NabavkaService nabavkaService;

    @Transactional(readOnly = true)
    public List<UgovorDto> getByDobavljac(Long dobavljacId) {
        return ugovorRepository.findByDobavljacDobavljacId(dobavljacId).stream()
                .map(this::toDto)
                .toList();
    }

    public UgovorDto create(UgovorDto dto) {
        Ugovor entity = Ugovor.builder()
                .dobavljac(dobavljacService.findEntity(dto.getDobavljacId()))
                .dokumentUrl(dto.getDokumentUrl())
                .status(dto.getStatus())
                .vaziDo(dto.getVaziDo())
                .build();

        if (dto.getNabavkaId() != null) {
            entity.setNabavka(nabavkaService.findEntity(dto.getNabavkaId()));
        }

        return toDto(ugovorRepository.save(entity));
    }

    private UgovorDto toDto(Ugovor entity) {
        return UgovorDto.builder()
                .ugovorId(entity.getUgovorId())
                .dobavljacId(entity.getDobavljac().getDobavljacId())
                .nabavkaId(entity.getNabavka() != null ? entity.getNabavka().getNabavkaId() : null)
                .dokumentUrl(entity.getDokumentUrl())
                .status(entity.getStatus())
                .kreiranAt(entity.getKreiranAt())
                .vaziDo(entity.getVaziDo())
                .build();
    }
}
