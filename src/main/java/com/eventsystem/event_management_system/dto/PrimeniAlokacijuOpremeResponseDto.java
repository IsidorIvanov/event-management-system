package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrimeniAlokacijuOpremeResponseDto {

    private PlanAlokacijeOpremeDto plan;

    @Builder.Default
    private List<DodelaOpremeDto> kreiraneDodele = new ArrayList<>();
}
