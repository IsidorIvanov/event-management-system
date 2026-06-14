package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UgovorDto {

    private Long ugovorId;

    private String brojUgovora;

    @NotNull(message = "ID dobavljača je obavezan")
    private Long dobavljacId;

    private String dobavljacNaziv;

    private Long dogadjajId;

    private String dogadjajNaziv;

    private Long nabavkaId;

    private LocalDate datumPotpisivanja;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal vrednost;

    private String predmet;

    private String usloviPlacanja;

    @Size(max = 500)
    private String dokumentUrl;

    private StatusUgovora status;

    private LocalDateTime kreiranAt;

    private LocalDate vaziDo;

    private LocalDateTime updatedAt;
}
