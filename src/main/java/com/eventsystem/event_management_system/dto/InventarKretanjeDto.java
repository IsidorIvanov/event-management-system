package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.TipInventarKretanja;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventarKretanjeDto {

    private Long kretanjeId;
    private Long opremaId;
    private String opremaNaziv;
    private TipInventarKretanja tip;
    private Integer kolicina;
    private Long porudzbenicaId;
    private Long dodelaId;
    private Long kreiraoId;
    private LocalDateTime kreiranAt;
    private String napomena;
}
