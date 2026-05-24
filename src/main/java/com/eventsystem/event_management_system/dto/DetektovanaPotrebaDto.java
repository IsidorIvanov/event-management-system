package com.eventsystem.event_management_system.dto;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetektovanaPotrebaDto {

    private String nazivResursa;
    private Integer kolicina;
    private String razlog;
    private String prioritet;
    private boolean pokrivenaUCenovniku;
    private Long preporuceniCenovnikId;
}
