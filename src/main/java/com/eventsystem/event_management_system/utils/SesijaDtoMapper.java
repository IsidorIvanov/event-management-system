package com.eventsystem.event_management_system.utils;

import com.eventsystem.event_management_system.dto.GovornikDto;
import com.eventsystem.event_management_system.dto.SesijaDto;
import com.eventsystem.event_management_system.model.Sesija;
import lombok.NoArgsConstructor;

import java.util.stream.Collectors;

@NoArgsConstructor
public class SesijaDtoMapper {

    public static SesijaDto toDto(Sesija s) {
        SesijaDto dto = new SesijaDto(
                s.getDogadjaj().getDogadjajId(),
                s.getSala().getId().getLokacijaId(),
                s.getSala().getId().getNazivSale(),
                s.getNaziv(),
                s.getDatum(),
                s.getVremePocetka(),
                s.getVremeZavrsetka(),
                s.getTip(),
                s.getKapacitet(),
                s.getOpis()
        );
        dto.setSesijaId(s.getSesijaId());
        dto.setGovornici(
                s.getGovornici().stream()
                        .map(g -> GovornikDto.builder()
                                .ime(g.getIme())
                                .prezime(g.getPrezime())
                                .kompanija(g.getKompanija())
                                .pozicija(g.getPozicija())
                                .biografija(g.getBiografija())
                                .email(g.getEmail())
                                .honorar(g.getHonorar())
                                .build())
                        .collect(Collectors.toSet())
        );
        return dto;
    }
}