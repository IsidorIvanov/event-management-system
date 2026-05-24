package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.model.Korisnik;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.repository.KorisnikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final KorisnikRepository korisnikRepository;

    public Korisnik getCurrentKorisnik() {
        String email = getCurrentEmail();
        return korisnikRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronadjen: " + email));
    }

    public Zaposleni getCurrentZaposleni() {
        Korisnik korisnik = getCurrentKorisnik();
        if (!(korisnik instanceof Zaposleni zaposleni)) {
            throw new RuntimeException("Samo zaposleni mogu da vrsaju ovu operaciju.");
        }
        return zaposleni;
    }

    private String getCurrentEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Korisnik nije autentifikovan.");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }
}
