package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KontaktDobavljacaDto {

    private Long nabavkaId;
    private Long dobavljacId;
    private String dobavljacNaziv;
    private String primaocEmail;
    private String subject;
    private String body;
    private boolean poslato;
    private LocalDateTime vremePripreme;
    private String napomena;
}
