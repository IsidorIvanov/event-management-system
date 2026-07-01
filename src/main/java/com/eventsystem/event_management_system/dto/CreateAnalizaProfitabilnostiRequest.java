package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAnalizaProfitabilnostiRequest {

    @Size(max = 5000, message = "Napomene mogu imati najvise 5000 karaktera")
    private String napomene;
}
