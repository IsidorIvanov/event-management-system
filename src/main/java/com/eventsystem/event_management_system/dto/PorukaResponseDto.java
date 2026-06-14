package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PorukaResponseDto {
    private Long porukaId;
    private Long posiljalacId;
    private String posiljalacIme;
    private String posiljalacPrezime;
    private Long primalacId;
    private String primalacIme;
    private String primalacPrezime;
    private String sadrzaj;
    private LocalDateTime vremeSlamja;
    private boolean procitano;
}

