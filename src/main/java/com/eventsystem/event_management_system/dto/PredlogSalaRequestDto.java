package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.TipSesije;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PredlogSalaRequestDto {

    @NotNull
    private Long dogadjajId;

    private Long sesijaId;

    @NotNull
    private Long lokacijaId;

    @NotNull
    private LocalDate datum;

    @NotNull
    private LocalTime vremePocetka;

    @NotNull
    private LocalTime vremeZavrsetka;

    @NotNull
    private TipSesije tip;

    @NotNull
    @Min(1)
    private Integer kapacitet;
}
