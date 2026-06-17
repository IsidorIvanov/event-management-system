package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PredlogSalaResponseDto {

    private Long dogadjajId;
    private Long sesijaId;

    @Builder.Default
    private List<PredlogSalaDto> predlozi = new ArrayList<>();

    @Builder.Default
    private List<String> upozorenjaOpreme = new ArrayList<>();
}
