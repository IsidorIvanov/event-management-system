package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttachDocumentRequest {

    @NotBlank(message = "URL dokumenta je obavezan")
    @Size(max = 500, message = "URL dokumenta može imati najviše 500 karaktera")
    private String dokumentUrl;
}
