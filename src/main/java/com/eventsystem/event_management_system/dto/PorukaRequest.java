package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PorukaRequest {
    @NotNull
    private Long primalacId;
    @NotBlank
    private String sadrzaj;
}

