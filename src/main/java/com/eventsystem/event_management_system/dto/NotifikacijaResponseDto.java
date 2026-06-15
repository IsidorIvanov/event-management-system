package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
import com.eventsystem.event_management_system.utils.enums.StatusNotifikacije;
import com.eventsystem.event_management_system.utils.enums.TipNotifikacije;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NotifikacijaResponseDto {
    private Long notifikacijaId;
    private TipNotifikacije tip;
    private KanalNotifikacije kanal;
    private String sadrzaj;
    private LocalDateTime vremeSlanja;
    private StatusNotifikacije status;
    private Long dogadjajId;
    private String dogadjajNaziv;
}
