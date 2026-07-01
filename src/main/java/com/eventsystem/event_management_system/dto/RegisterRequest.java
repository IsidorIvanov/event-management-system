package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.TipKorisnika;
import com.eventsystem.event_management_system.utils.enums.UlogaZaposlenog;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RegisterRequest {

    @NotBlank(message = "Ime je obavezno")
    private String ime;

    @NotBlank(message = "Prezime je obavezno")
    private String prezime;

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email format nije validan")
    private String email;

    @NotBlank(message = "Lozinka je obavezna")
    @Size(min = 6, message = "Lozinka mora imati najmanje 6 karaktera")
    private String lozinka;

    private String telefon;

    @NotNull(message = "Tip korisnika je obavezan")
    private TipKorisnika tipKorisnika;

    // --- Polja za ZAPOSLENI ---
    private UlogaZaposlenog uloga;
    private String pozicija;
    private LocalDate datumZaposlenja;

    // --- Polja za KLIJENT ---
    private String nazivFirme;
    private String pib;
    private String adresa;
    private String grad;
    private String kontaktOsoba;

    // --- Polja za UCESNIK ---
    private String kompanija;
    // pozicija se deli sa ZAPOSLENI
    private LocalDate datumRodjenja;
    // Interesovanja koja se koriste za sistem preporuka događaja
    private List<String> interesi;
}
