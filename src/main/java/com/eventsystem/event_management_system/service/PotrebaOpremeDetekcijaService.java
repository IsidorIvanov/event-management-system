package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DetektovanaPotrebaDto;
import com.eventsystem.event_management_system.dto.PotrebnaStavkaDto;
import com.eventsystem.event_management_system.dto.ValidacijaPotrebaDto;
import com.eventsystem.event_management_system.model.Cenovnik;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.repository.CenovnikRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import com.eventsystem.event_management_system.utils.enums.TipSesije;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PotrebaOpremeDetekcijaService {

    private final DogadjajRepository dogadjajRepository;
    private final SesijaRepository sesijaRepository;
    private final CenovnikRepository cenovnikRepository;

    @Transactional(readOnly = true)
    public List<DetektovanaPotrebaDto> detektujPotrebe(Long dogadjajId) {
        Dogadjaj dogadjaj = dogadjajRepository.findByIdWithLokacija(dogadjajId)
                .orElseThrow(() -> new RuntimeException("Dogadjaj nije pronadjen sa id: " + dogadjajId));
        List<Sesija> sesije = sesijaRepository.findAllByDogadjaj_DogadjajId(dogadjajId);

        Map<String, DetektovanaPotrebaDto> agregirane = new LinkedHashMap<>();

        dodajPotrebu(agregirane, "Zvučnici PA", 1, "Osnovna audio oprema za događaj", "VISOK");

        if (dogadjaj.getMaksKapacitet() != null && dogadjaj.getMaksKapacitet() >= 200) {
            int ledKom = Math.max(1, dogadjaj.getMaksKapacitet() / 200);
            dodajPotrebu(agregirane, "LED rasveta", ledKom,
                    "Kapacitet događaja >= 200 (" + dogadjaj.getMaksKapacitet() + ")", "VISOK");
        }

        if (dogadjaj.getMaksKapacitet() != null && dogadjaj.getMaksKapacitet() >= 500) {
            int bina = Math.max(1, dogadjaj.getMaksKapacitet() / 500);
            dodajPotrebu(agregirane, "Bina modul", bina,
                    "Veliki događaj — kapacitet " + dogadjaj.getMaksKapacitet(), "SREDNJI");
        }

        long keynotePanel = sesije.stream()
                .filter(s -> s.getTip() == TipSesije.KEYNOTE || s.getTip() == TipSesije.PANEL)
                .count();
        if (keynotePanel > 0) {
            dodajPotrebu(agregirane, "Mikrofoni bežični", (int) Math.min(3, Math.max(1, keynotePanel)),
                    "KEYNOTE/PANEL sesije: " + keynotePanel, "VISOK");
        }

        long workshop = sesije.stream().filter(s -> s.getTip() == TipSesije.WORKSHOP).count();
        if (workshop >= 2) {
            dodajPotrebu(agregirane, "Mikrofoni bežični", 1,
                    "Dodatni set za " + workshop + " WORKSHOP sesija", "SREDNJI");
        }

        if (sesije.size() >= 5) {
            dodajPotrebu(agregirane, "LED rasveta", 1,
                    "Više od 5 sesija — dodatna rasveta", "SREDNJI");
        }

        return obogatiCenovnikom(new ArrayList<>(agregirane.values()));
    }

    @Transactional(readOnly = true)
    public ValidacijaPotrebaDto validirajPotrebe(Long dogadjajId) {
        return validirajListu(dogadjajId, detektujPotrebe(dogadjajId));
    }

    @Transactional(readOnly = true)
    public ValidacijaPotrebaDto validirajUnetePotrebe(Long dogadjajId, List<PotrebnaStavkaDto> unete) {
        List<DetektovanaPotrebaDto> kaoDetektovane = unete.stream()
                .map(u -> DetektovanaPotrebaDto.builder()
                        .nazivResursa(u.getNazivResursa())
                        .kolicina(u.getKolicina())
                        .razlog("Ručno uneta stavka")
                        .prioritet("KORISNIK")
                        .build())
                .toList();
        return validirajListu(dogadjajId, obogatiCenovnikom(kaoDetektovane));
    }

    private ValidacijaPotrebaDto validirajListu(Long dogadjajId, List<DetektovanaPotrebaDto> potrebe) {
        Dogadjaj dogadjaj = dogadjajRepository.findById(dogadjajId)
                .orElseThrow(() -> new RuntimeException("Dogadjaj nije pronadjen sa id: " + dogadjajId));

        List<String> upozorenja = new ArrayList<>();
        List<String> nepokrivene = new ArrayList<>();
        int pokrivene = 0;

        for (DetektovanaPotrebaDto p : potrebe) {
            if (p.isPokrivenaUCenovniku()) {
                pokrivene++;
            } else {
                nepokrivene.add(p.getNazivResursa());
                upozorenja.add("Resurs \"" + p.getNazivResursa() + "\" nije u cenovniku aktivnih dobavljača.");
            }
        }

        int ukupno = potrebe.size();
        boolean validno = nepokrivene.isEmpty() && ukupno > 0;
        if (ukupno == 0) {
            upozorenja.add("Nisu detektovane potrebe za opremu za ovaj događaj.");
        }

        return ValidacijaPotrebaDto.builder()
                .dogadjajId(dogadjajId)
                .dogadjajNaziv(dogadjaj.getNaziv())
                .ukupnoPotreba(ukupno)
                .pokrivenePotrebe(pokrivene)
                .validno(validno)
                .detektovanePotrebe(potrebe)
                .upozorenja(upozorenja)
                .nepokriveneStavke(nepokrivene)
                .build();
    }

    private List<DetektovanaPotrebaDto> obogatiCenovnikom(List<DetektovanaPotrebaDto> potrebe) {
        List<Cenovnik> svi = cenovnikRepository.findSveDostupne();
        for (DetektovanaPotrebaDto p : potrebe) {
            Optional<Cenovnik> najbolja = svi.stream()
                    .filter(c -> naziviSePoklapaju(c.getNazivResursa(), p.getNazivResursa()))
                    .min(Comparator.comparing(Cenovnik::getCenaJedinicna));
            najbolja.ifPresent(c -> {
                p.setPokrivenaUCenovniku(true);
                p.setPreporuceniCenovnikId(c.getCenovnikId());
            });
        }
        return potrebe;
    }

    private boolean naziviSePoklapaju(String izCenovnika, String potreba) {
        String a = izCenovnika.toLowerCase().trim();
        String b = potreba.toLowerCase().trim();
        return a.equals(b) || a.contains(b) || b.contains(a);
    }

    private void dodajPotrebu(Map<String, DetektovanaPotrebaDto> mapa,
                              String naziv, int kolicina, String razlog, String prioritet) {
        String kljuc = naziv.toLowerCase();
        if (mapa.containsKey(kljuc)) {
            DetektovanaPotrebaDto postojeca = mapa.get(kljuc);
            postojeca.setKolicina(postojeca.getKolicina() + kolicina);
            postojeca.setRazlog(postojeca.getRazlog() + "; " + razlog);
        } else {
            mapa.put(kljuc, DetektovanaPotrebaDto.builder()
                    .nazivResursa(naziv)
                    .kolicina(kolicina)
                    .razlog(razlog)
                    .prioritet(prioritet)
                    .pokrivenaUCenovniku(false)
                    .build());
        }
    }
}
