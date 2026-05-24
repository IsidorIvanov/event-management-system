package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UgovorDto {

    private Long ugovorId;

    @NotNull(message = "ID dobavljača je obavezan")
    private Long dobavljacId;

    private Long nabavkaId;

    @Size(max = 500)
    private String dokumentUrl;

    private StatusUgovora status;

    private LocalDateTime kreiranAt;

    private LocalDate vaziDo;
}
