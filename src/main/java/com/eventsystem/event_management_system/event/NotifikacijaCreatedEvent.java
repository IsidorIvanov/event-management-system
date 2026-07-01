package com.eventsystem.event_management_system.event;

import com.eventsystem.event_management_system.dto.EmailDetalj;
import com.eventsystem.event_management_system.dto.NotifikacijaResponseDto;
import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Domain event objavljen kada je notifikacija sačuvana u bazi. Isporuka kroz
 * kanal (PUSH/EMAIL) se radi u {@code NotifikacijaEventListener} tek
 * {@code AFTER_COMMIT}, kako bi se obaveštenje poslalo samo ako je transakcija
 * koja ga je izazvala zaista uspela.
 *
 * <p>{@code emailNaslov} je opciono: ako je postavljen, notifikacija se pored
 * primarnog kanala dodatno isporučuje i mejlom (npr. D3 — oslobođeno mesto).</p>
 */
@Getter
@RequiredArgsConstructor
public class NotifikacijaCreatedEvent {
    private final String primalacEmail;
    private final String primalacIme;
    private final KanalNotifikacije kanal;
    private final String emailNaslov;
    private final String emailPoruka;
    private final List<EmailDetalj> emailDetalji;
    private final NotifikacijaResponseDto dto;
}
