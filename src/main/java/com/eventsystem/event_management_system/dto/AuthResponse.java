package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.TipKorisnika;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long korisnikId;
    private String ime;
    private String prezime;
    private String email;
    private TipKorisnika tipKorisnika;
    private String uloga; // null za KLIJENT i UCESNIK
}
