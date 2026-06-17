package com.eventsystem.event_management_system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OptimizujAlokacijuRequestDto {

    private Long nabavkaId;

    @NotEmpty(message = "Lista potrebnih stavki ne sme biti prazna")
    @Valid
    private List<PotrebnaStavkaDto> potrebneStavke;
}
