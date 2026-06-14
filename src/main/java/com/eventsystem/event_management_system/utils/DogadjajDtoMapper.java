package com.eventsystem.event_management_system.utils;

import com.eventsystem.event_management_system.dto.DogadjajResponseDto;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Lokacija;
import lombok.NoArgsConstructor;

import java.util.HashSet;

@NoArgsConstructor
public class DogadjajDtoMapper {

    public static DogadjajResponseDto toResponseDto(Dogadjaj d) {
        Lokacija l = d.getLokacija();
        return new DogadjajResponseDto(
                d.getDogadjajId(),
                d.getNaziv(),
                d.getDatumPocetka().toString(),
                d.getDatumZavrsetka().toString(),
                d.getMaksKapacitet(),
                d.getOpis(),
                d.getStatus(),
                l != null ? l.getNaziv() : "",
                l != null ? l.getGrad() : "",
                l != null ? l.getDrzava() : "",
                l != null ? l.getAdresa() : "",
                new HashSet<>(d.getTagovi())
        );
    }
}
