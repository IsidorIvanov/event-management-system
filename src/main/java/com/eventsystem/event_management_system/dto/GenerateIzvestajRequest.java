package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.FormatIzvestaja;
import com.eventsystem.event_management_system.utils.enums.TipIzvestaja;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateIzvestajRequest {

    @NotNull(message = "Tip izveštaja je obavezan.")
    private TipIzvestaja tip;

    @NotNull(message = "Format izveštaja je obavezan.")
    private FormatIzvestaja format;

    private Long dogadjajId;
    private Long klijentId;
    private Long dobavljacId;
    private LocalDate periodOd;
    private LocalDate periodDo;
}
