package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.LoginRequest;
import com.eventsystem.event_management_system.dto.RegisterRequest;
import com.eventsystem.event_management_system.dto.AuthResponse;
import com.eventsystem.event_management_system.model.*;
import com.eventsystem.event_management_system.utils.enums.StatusKorisnika;
import com.eventsystem.event_management_system.repository.KorisnikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KorisnikRepository korisnikRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (korisnikRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email " + request.getEmail() + " je već registrovan");
        }

        Korisnik korisnik = switch (request.getTipKorisnika()) {
            case ZAPOSLENI -> createZaposleni(request);
            case KLIJENT -> createKlijent(request);
            case UCESNIK -> createUcesnik(request);
        };

        korisnikRepository.save(korisnik);

        UserDetails userDetails = userDetailsService.loadUserByUsername(korisnik.getEmail());
        Map<String, Object> claims = new HashMap<>();
        claims.put("tipKorisnika", korisnik.getTipKorisnika().name());
        claims.put("korisnikId", korisnik.getKorisnikId());
        if (korisnik instanceof Zaposleni z && z.getUloga() != null) {
            claims.put("uloga", z.getUloga().name());
        }

        String token = jwtService.generateToken(claims, userDetails);
        return buildAuthResponse(korisnik, token);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getLozinka())
        );

        Korisnik korisnik = korisnikRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen"));

        if (korisnik.getStatus() != StatusKorisnika.AKTIVAN) {
            throw new RuntimeException("Nalog nije aktivan. Status: " + korisnik.getStatus());
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(korisnik.getEmail());
        Map<String, Object> claims = new HashMap<>();
        claims.put("tipKorisnika", korisnik.getTipKorisnika().name());
        claims.put("korisnikId", korisnik.getKorisnikId());
        if (korisnik instanceof Zaposleni z && z.getUloga() != null) {
            claims.put("uloga", z.getUloga().name());
        }

        String token = jwtService.generateToken(claims, userDetails);
        return buildAuthResponse(korisnik, token);
    }

    // ============ PRIVATNE METODE ============

    private Zaposleni createZaposleni(RegisterRequest req) {
        Zaposleni z = new Zaposleni();
        setCommonFields(z, req);
        z.setUloga(req.getUloga());
        z.setPozicija(req.getPozicija());
        z.setDatumZaposlenja(req.getDatumZaposlenja() != null ? req.getDatumZaposlenja() : LocalDate.now());
        return z;
    }

    private Klijent createKlijent(RegisterRequest req) {
        Klijent k = new Klijent();
        setCommonFields(k, req);
        k.setNazivFirme(req.getNazivFirme());
        k.setPib(req.getPib());
        k.setAdresa(req.getAdresa());
        k.setGrad(req.getGrad());
        k.setKontaktOsoba(req.getKontaktOsoba());
        return k;
    }

    private Ucesnik createUcesnik(RegisterRequest req) {
        Ucesnik u = new Ucesnik();
        setCommonFields(u, req);
        u.setKompanija(req.getKompanija());
        u.setPozicija(req.getPozicija());
        u.setDatumRodjenja(req.getDatumRodjenja());
        return u;
    }

    private void setCommonFields(Korisnik k, RegisterRequest req) {
        k.setIme(req.getIme());
        k.setPrezime(req.getPrezime());
        k.setEmail(req.getEmail());
        k.setLozinkaHash(passwordEncoder.encode(req.getLozinka()));
        k.setTelefon(req.getTelefon());
        k.setDatumRegistracije(LocalDate.now());
        k.setStatus(StatusKorisnika.AKTIVAN);
    }

    private AuthResponse buildAuthResponse(Korisnik korisnik, String token) {
        String uloga = null;
        if (korisnik instanceof Zaposleni z && z.getUloga() != null) {
            uloga = z.getUloga().name();
        }
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .korisnikId(korisnik.getKorisnikId())
                .ime(korisnik.getIme())
                .prezime(korisnik.getPrezime())
                .email(korisnik.getEmail())
                .tipKorisnika(korisnik.getTipKorisnika())
                .uloga(uloga)
                .build();
    }
}
