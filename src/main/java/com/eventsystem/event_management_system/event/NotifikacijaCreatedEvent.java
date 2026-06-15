package com.eventsystem.event_management_system.event;

import com.eventsystem.event_management_system.dto.NotifikacijaResponseDto;
import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Domain event objavljen kada je notifikacija sačuvana u bazi. Isporuka kroz
 * kanal (PUSH/EMAIL) se radi u {@code NotifikacijaEventListener} tek
 * {@code AFTER_COMMIT}, kako bi se obaveštenje poslalo samo ako je transakcija
 * koja ga je izazvala zaista uspela.
 */
@Getter
@RequiredArgsConstructor
public class NotifikacijaCreatedEvent {
    private final String primalacEmail;
    private final KanalNotifikacije kanal;
    private final NotifikacijaResponseDto dto;
}
