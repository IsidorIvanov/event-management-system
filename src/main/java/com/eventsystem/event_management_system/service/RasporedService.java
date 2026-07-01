package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.RasporedSesijaDto;
import com.eventsystem.event_management_system.model.*;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import com.eventsystem.event_management_system.repository.UcesnikSesijaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RasporedService {

    private final UcesnikSesijaRepository ucesnikSesijaRepository;
    private final SesijaRepository sesijaRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public RasporedSesijaDto addSesijaToRaspored(Long sesijaId) {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        if (!(korisnik instanceof Ucesnik ucesnik)) {
            throw new RuntimeException("Samo učesnici mogu dodavati sesije u raspored.");
        }

        Sesija sesija = sesijaRepository.findByIdWithGovornici(sesijaId)
                .orElseThrow(() -> new RuntimeException("Sesija nije pronađena sa id: " + sesijaId));

        if (ucesnikSesijaRepository.existsByUcesnik_KorisnikIdAndSesija_SesijaId(
                ucesnik.getKorisnikId(), sesijaId)) {
            throw new RuntimeException("Sesija je već u vašem rasporedu.");
        }

        UcesnikSesija entry = UcesnikSesija.builder()
                .ucesnik(ucesnik)
                .sesija(sesija)
                .build();

        ucesnikSesijaRepository.save(entry);
        return toDto(sesija);
    }

    @Transactional
    public void removeSesijaFromRaspored(Long sesijaId) {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        if (!(korisnik instanceof Ucesnik ucesnik)) {
            throw new RuntimeException("Samo učesnici mogu uklanjati sesije iz rasporeda.");
        }
        ucesnikSesijaRepository.deleteByUcesnik_KorisnikIdAndSesija_SesijaId(
                ucesnik.getKorisnikId(), sesijaId);
    }

    @Transactional(readOnly = true)
    public List<RasporedSesijaDto> getMojRaspored() {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        return ucesnikSesijaRepository.findByUcesnikIdWithDetails(korisnik.getKorisnikId())
                .stream()
                .map(us -> toDto(us.getSesija()))
                .sorted((a, b) -> {
                    int dateCmp = a.getDatum().compareTo(b.getDatum());
                    if (dateCmp != 0) return dateCmp;
                    return a.getVremePocetka().compareTo(b.getVremePocetka());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Long> getMojRasporedIds() {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        return ucesnikSesijaRepository.findByUcesnikIdWithDetails(korisnik.getKorisnikId())
                .stream()
                .map(us -> us.getSesija().getSesijaId())
                .collect(Collectors.toList());
    }

    private RasporedSesijaDto toDto(Sesija s) {
        Dogadjaj d = s.getDogadjaj();
        Sala sala = s.getSala();
        Lokacija lok = sala.getLokacija();

        List<RasporedSesijaDto.GovornikImeDto> govornici = s.getGovornici().stream()
                .map(g -> RasporedSesijaDto.GovornikImeDto.builder()
                        .govornikId(g.getGovornikId())
                        .ime(g.getIme())
                        .prezime(g.getPrezime())
                        .kompanija(g.getKompanija())
                        .pozicija(g.getPozicija())
                        .build())
                .collect(Collectors.toList());

        return RasporedSesijaDto.builder()
                .sesijaId(s.getSesijaId())
                .naziv(s.getNaziv())
                .datum(s.getDatum())
                .vremePocetka(s.getVremePocetka())
                .vremeZavrsetka(s.getVremeZavrsetka())
                .tip(s.getTip())
                .kapacitet(s.getKapacitet())
                .opis(s.getOpis())
                .nazivSale(sala.getId().getNazivSale())
                .dogadjajId(d.getDogadjajId())
                .dogadjajNaziv(d.getNaziv())
                .lokacijaId(lok.getLokacijaId())
                .lokacijaNaziv(lok.getNaziv())
                .lokacijaGrad(lok.getGrad())
                .govornici(govornici)
                .build();
    }
}

