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
import com.eventsystem.event_management_system.utils.TekstNormalizacija;
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
        List<Cenovnik> cenovnik = cenovnikRepository.findSveDostupne();
        if (cenovnik.isEmpty()) {
            return List.of();
        }

        Dogadjaj dogadjaj = dogadjajRepository.findByIdWithLokacija(dogadjajId)
                .orElseThrow(() -> new RuntimeException("Dogadjaj nije pronadjen sa id: " + dogadjajId));
        List<Sesija> sesije = sesijaRepository.findAllByDogadjaj_DogadjajId(dogadjajId);

        Map<String, PotencijalnaPotreba> agregirane = new LinkedHashMap<>();

        predloziAkoPostojiUCenovniku(cenovnik, agregirane, "zvucnici", 1,
                "Osnovna audio oprema za događaj", "VISOK");

        if (dogadjaj.getMaksKapacitet() != null && dogadjaj.getMaksKapacitet() >= 200) {
            int ledKom = Math.max(1, dogadjaj.getMaksKapacitet() / 200);
            predloziAkoPostojiUCenovniku(cenovnik, agregirane, "led", ledKom,
                    "Kapacitet događaja >= 200 (" + dogadjaj.getMaksKapacitet() + ")", "VISOK");
        }

        if (dogadjaj.getMaksKapacitet() != null && dogadjaj.getMaksKapacitet() >= 500) {
            int bina = Math.max(1, dogadjaj.getMaksKapacitet() / 500);
            predloziAkoPostojiUCenovniku(cenovnik, agregirane, "bina", bina,
                    "Veliki događaj — kapacitet " + dogadjaj.getMaksKapacitet(), "SREDNJI");
        }

        long keynotePanel = sesije.stream()
                .filter(s -> s.getTip() == TipSesije.KEYNOTE || s.getTip() == TipSesije.PANEL)
                .count();
        if (keynotePanel > 0) {
            predloziAkoPostojiUCenovniku(cenovnik, agregirane, "mikrofon", (int) Math.min(3, Math.max(1, keynotePanel)),
                    "KEYNOTE/PANEL sesije: " + keynotePanel, "VISOK");
        }

        long workshop = sesije.stream().filter(s -> s.getTip() == TipSesije.WORKSHOP).count();
        if (workshop >= 2) {
            predloziAkoPostojiUCenovniku(cenovnik, agregirane, "mikrofon", 1,
                    "Dodatni set za " + workshop + " WORKSHOP sesija", "SREDNJI");
        }

        if (sesije.size() >= 5) {
            predloziAkoPostojiUCenovniku(cenovnik, agregirane, "led", 1,
                    "Više od 5 sesija — dodatna rasveta", "SREDNJI");
        }

        return agregirane.values().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ValidacijaPotrebaDto validirajPotrebe(Long dogadjajId) {
        List<DetektovanaPotrebaDto> potrebe = detektujPotrebe(dogadjajId);
        return validirajListu(dogadjajId, potrebe);
    }

    @Transactional(readOnly = true)
    public ValidacijaPotrebaDto validirajUnetePotrebe(Long dogadjajId, List<PotrebnaStavkaDto> unete) {
        List<Cenovnik> cenovnik = cenovnikRepository.findSveDostupne();
        List<DetektovanaPotrebaDto> potrebe = unete.stream()
                .map(u -> mapirajNaCenovnik(u, cenovnik))
                .filter(Objects::nonNull)
                .toList();
        return validirajListu(dogadjajId, potrebe);
    }

    private DetektovanaPotrebaDto mapirajNaCenovnik(PotrebnaStavkaDto uneta, List<Cenovnik> cenovnik) {
        Optional<Cenovnik> match = nadjiNajboljuPonudu(cenovnik, uneta.getNazivResursa());
        if (match.isEmpty()) {
            return null;
        }
        Cenovnik c = match.get();
        return DetektovanaPotrebaDto.builder()
                .nazivResursa(c.getNazivResursa())
                .kolicina(uneta.getKolicina())
                .razlog("Ručno uneta stavka")
                .prioritet("KORISNIK")
                .pokrivenaUCenovniku(true)
                .preporuceniCenovnikId(c.getCenovnikId())
                .build();
    }

    private ValidacijaPotrebaDto validirajListu(Long dogadjajId, List<DetektovanaPotrebaDto> potrebe) {
        Dogadjaj dogadjaj = dogadjajRepository.findById(dogadjajId)
                .orElseThrow(() -> new RuntimeException("Dogadjaj nije pronadjen sa id: " + dogadjajId));

        List<String> upozorenja = new ArrayList<>();
        int ukupno = potrebe.size();
        if (ukupno == 0) {
            upozorenja.add("Nema stavki u cenovniku koje odgovaraju potrebama ovog događaja.");
        }

        return ValidacijaPotrebaDto.builder()
                .dogadjajId(dogadjajId)
                .dogadjajNaziv(dogadjaj.getNaziv())
                .ukupnoPotreba(ukupno)
                .pokrivenePotrebe(ukupno)
                .validno(ukupno > 0)
                .detektovanePotrebe(potrebe)
                .upozorenja(upozorenja)
                .nepokriveneStavke(List.of())
                .build();
    }

    private void predloziAkoPostojiUCenovniku(List<Cenovnik> cenovnik,
                                              Map<String, PotencijalnaPotreba> mapa,
                                              String kljucnaRec,
                                              int kolicina,
                                              String razlog,
                                              String prioritet) {
        nadjiNajboljuPonuduPoKljucnojReci(cenovnik, kljucnaRec).ifPresent(c -> {
            String kljuc = TekstNormalizacija.normalizuj(c.getNazivResursa());
            if (mapa.containsKey(kljuc)) {
                PotencijalnaPotreba postojeca = mapa.get(kljuc);
                postojeca.kolicina += kolicina;
                postojeca.razlog = postojeca.razlog + "; " + razlog;
            } else {
                mapa.put(kljuc, new PotencijalnaPotreba(c, kolicina, razlog, prioritet));
            }
        });
    }

    private Optional<Cenovnik> nadjiNajboljuPonudu(List<Cenovnik> cenovnik, String nazivResursa) {
        return cenovnik.stream()
                .filter(c -> TekstNormalizacija.naziviSePodudaraju(c.getNazivResursa(), nazivResursa))
                .min(Comparator.comparing(Cenovnik::getCenaJedinicna));
    }

    private Optional<Cenovnik> nadjiNajboljuPonuduPoKljucnojReci(List<Cenovnik> cenovnik, String kljucnaRec) {
        String k = TekstNormalizacija.normalizuj(kljucnaRec);
        return cenovnik.stream()
                .filter(c -> TekstNormalizacija.normalizuj(c.getNazivResursa()).contains(k))
                .min(Comparator.comparing(Cenovnik::getCenaJedinicna));
    }

    private DetektovanaPotrebaDto toDto(PotencijalnaPotreba p) {
        return DetektovanaPotrebaDto.builder()
                .nazivResursa(p.cenovnik.getNazivResursa())
                .kolicina(p.kolicina)
                .razlog(p.razlog)
                .prioritet(p.prioritet)
                .pokrivenaUCenovniku(true)
                .preporuceniCenovnikId(p.cenovnik.getCenovnikId())
                .build();
    }

    private static class PotencijalnaPotreba {
        final Cenovnik cenovnik;
        int kolicina;
        String razlog;
        final String prioritet;

        PotencijalnaPotreba(Cenovnik cenovnik, int kolicina, String razlog, String prioritet) {
            this.cenovnik = cenovnik;
            this.kolicina = kolicina;
            this.razlog = razlog;
            this.prioritet = prioritet;
        }
    }
}
