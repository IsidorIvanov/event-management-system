package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.StatusKorisnika;
import com.eventsystem.event_management_system.utils.enums.TipKorisnika;
import com.eventsystem.event_management_system.utils.enums.UlogaZaposlenog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Prikaz profila trenutno prijavljenog korisnika. Sadrži zajednička polja
 * i polja specifična za tip korisnika; nekorišćena polja ostaju null.
 */
@Data
@Builder
public class ProfilDto {

    private Long korisnikId;
    private String ime;
    private String prezime;
    private String email;
    private String telefon;
    private LocalDate datumRegistracije;
    private StatusKorisnika status;
    private TipKorisnika tipKorisnika;

    // --- ZAPOSLENI ---
    private UlogaZaposlenog uloga;
    private LocalDate datumZaposlenja;

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
