package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SelekcijaDobavljacaResponseDto {

    private PredlogDobavljacaDto predlog;

    @Builder.Default
    private List<PredlogDobavljacaDto> alternative = new ArrayList<>();
}
