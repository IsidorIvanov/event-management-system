package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StatusUpdateDto {

    @NotNull(message = "Status je obavezan")
    private String status;
}
