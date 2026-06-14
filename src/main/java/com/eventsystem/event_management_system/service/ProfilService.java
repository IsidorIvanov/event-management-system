package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.ProfilDto;
import com.eventsystem.event_management_system.dto.ProfilUpdateRequest;
import com.eventsystem.event_management_system.model.Klijent;
import com.eventsystem.event_management_system.model.Korisnik;
import com.eventsystem.event_management_system.model.Ucesnik;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.repository.KorisnikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfilService {

    private final KorisnikRepository korisnikRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public ProfilDto getMojProfil() {
        return toDto(currentUserService.getCurrentKorisnik());
    }

    @Transactional
    public ProfilDto azurirajMojProfil(ProfilUpdateRequest req) {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();

        String noviEmail = req.getEmail().trim();
        if (!noviEmail.equalsIgnoreCase(korisnik.getEmail())
                && korisnikRepository.existsByEmail(noviEmail)) {
            throw new RuntimeException("Email " + noviEmail + " je već registrovan");
        }

        korisnik.setIme(req.getIme().trim());
        korisnik.setPrezime(req.getPrezime().trim());
        korisnik.setEmail(noviEmail);
        korisnik.setTelefon(req.getTelefon());

        if (korisnik instanceof Zaposleni z) {
            z.setPozicija(req.getPozicija());
        } else if (korisnik instanceof Klijent k) {
            k.setNazivFirme(req.getNazivFirme());
            k.setPib(req.getPib());
            k.setAdresa(req.getAdresa());
            k.setGrad(req.getGrad());
            k.setKontaktOsoba(req.getKontaktOsoba());
        } else if (korisnik instanceof Ucesnik u) {
            u.setKompanija(req.getKompanija());
            u.setPozicija(req.getPozicija());
            u.setDatumRodjenja(req.getDatumRodjenja());
            u.setInteresi(normalizujInterese(req.getInteresi()));
        }

        korisnikRepository.save(korisnik);
        return toDto(korisnik);
    }

    // ============ PRIVATNE METODE ============

    private ProfilDto toDto(Korisnik korisnik) {
        ProfilDto.ProfilDtoBuilder builder = ProfilDto.builder()
                .korisnikId(korisnik.getKorisnikId())
                .ime(korisnik.getIme())
                .prezime(korisnik.getPrezime())
                .email(korisnik.getEmail())
                .telefon(korisnik.getTelefon())
                .datumRegistracije(korisnik.getDatumRegistracije())
                .status(korisnik.getStatus())
                .tipKorisnika(korisnik.getTipKorisnika());

        if (korisnik instanceof Zaposleni z) {
            builder.uloga(z.getUloga())
                    .pozicija(z.getPozicija())
                    .datumZaposlenja(z.getDatumZaposlenja());
        } else if (korisnik instanceof Klijent k) {
            builder.nazivFirme(k.getNazivFirme())
                    .pib(k.getPib())
                    .adresa(k.getAdresa())
                    .grad(k.getGrad())
                    .kontaktOsoba(k.getKontaktOsoba());
        } else if (korisnik instanceof Ucesnik u) {
            builder.kompanija(u.getKompanija())
                    .pozicija(u.getPozicija())
                    .datumRodjenja(u.getDatumRodjenja())
                    .interesi(new ArrayList<>(u.getInteresi()));
        }

        return builder.build();
    }

    /**
     * Čisti listu interesa: trimuje, izbacuje prazne i uklanja duplikate
     * (bez obzira na velika/mala slova), čuvajući originalni unos.
     */
    private Set<String> normalizujInterese(List<String> interesi) {
        Set<String> rezultat = new LinkedHashSet<>();
        if (interesi == null) {
            return rezultat;
        }
        Set<String> vidjeni = new HashSet<>();
        for (String interes : interesi) {
            if (interes == null) {
                continue;
            }
            String ocisceno = interes.trim();
            if (!ocisceno.isEmpty() && vidjeni.add(ocisceno.toLowerCase())) {
                rezultat.add(ocisceno);
            }
        }
        return rezultat;
    }
}
