package com.eventsystem.event_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaDostupnostResponseDto {

    private Long lokacijaId;
    private LocalDate datumOd;
    private LocalDate datumDo;
    private int satPocetka;
    private int satZavrsetka;

    @Builder.Default
    private List<SalaDostupnostDto> sale = new ArrayList<>();
}
