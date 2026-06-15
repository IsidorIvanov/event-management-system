package com.eventsystem.event_management_system.dto;

/**
 * Jedan red u „detalji" kartici email obaveštenja (oznaka → vrednost), npr.
 * „Novi datum" → „12.06.2026.". Omogućava da notifikacija prosledi strukturirane
 * podatke koje email prikazuje pregledno, umesto samo u rečenici.
 */
public record EmailDetalj(String oznaka, String vrednost) {}
