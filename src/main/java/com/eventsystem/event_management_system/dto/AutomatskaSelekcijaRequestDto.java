package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.KriterijumSelekcijeDobavljaca;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutomatskaSelekcijaRequestDto {

    @NotNull(message = "ID nabavke je obavezan")
    private Long nabavkaId;

    @NotNull(message = "Kriterijum selekcije je obavezan")
    private KriterijumSelekcijeDobavljaca kriterijum;

    @NotEmpty(message = "Lista potrebnih stavki ne sme biti prazna")
    @Valid
    private List<PotrebnaStavkaDto> potrebneStavke;
}
