package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.model.Korisnik;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.repository.KorisnikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final KorisnikRepository korisnikRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Korisnik korisnik = korisnikRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Korisnik sa emailom " + email + " nije pronađen"));

        return new org.springframework.security.core.userdetails.User(
                korisnik.getEmail(),
                korisnik.getLozinkaHash(),
                getAuthorities(korisnik)
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Korisnik korisnik) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Osnovna rola po tipu korisnika: ROLE_ZAPOSLENI, ROLE_KLIJENT, ROLE_UCESNIK
        authorities.add(new SimpleGrantedAuthority("ROLE_" + korisnik.getTipKorisnika().name()));

        // Ako je zaposleni, dodaj i ulogu: ROLE_MENADZER_DOGADJAJA, ROLE_FINANSIJSKI_KONTROLOR itd.
        if (korisnik instanceof Zaposleni zaposleni && zaposleni.getUloga() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + zaposleni.getUloga().name()));
        }

        return authorities;
    }
}
