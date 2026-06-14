package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Podaci koje korisnik može sam da izmeni na svom profilu.
 * Tip korisnika, uloga i status se ne menjaju ovde.
 */
@Data
public class ProfilUpdateRequest {

    @NotBlank(message = "Ime je obavezno")
    private String ime;

    @NotBlank(message = "Prezime je obavezno")
    private String prezime;

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email format nije validan")
    private String email;

    private String telefon;

    // --- KLIJENT ---
    private String nazivFirme;
    private String pib;
    private String adresa;
    private String grad;
    private String kontaktOsoba;

    // --- UCESNIK ---
    private String kompanija;
    private LocalDate datumRodjenja;
    private List<String> interesi;

    // --- deljeno (ZAPOSLENI / UCESNIK) ---
    private String pozicija;
}
