package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DogadjajKontaktiDto;
import com.eventsystem.event_management_system.dto.KontaktKorisnikaDto;
import com.eventsystem.event_management_system.dto.PreporukaDto;
import com.eventsystem.event_management_system.model.*;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.PreporukaRepository;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.repository.ZaposleniRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import com.eventsystem.event_management_system.utils.enums.StatusRegistracije;
import com.eventsystem.event_management_system.utils.enums.UlogaZaposlenog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreporukaService {

    private final PreporukaRepository preporukaRepository;
    private final DogadjajRepository dogadjajRepository;
    private final RegistracijaRepository registracijaRepository;
    private final ZaposleniRepository zaposleniRepository;
    private final CurrentUserService currentUserService;

    /**
     * Događaj se preporučuje samo ako se bar jedan tag poklapa sa interesovanjem
     * učesnika i pri tom poklapanje je "značajno" — skor je bar {@value MIN_SKOR} %
     * ili se poklapaju bar {@value MIN_POKLAPANJA} taga.
     */
    private static final double MIN_SKOR = 30.0;
    private static final int MIN_POKLAPANJA = 2;

    /** Materijalizovani podaci o jednoj preporuci — bez ijedne lazy kolekcije. */
    private record PreporukaPodaci(Preporuka preporuka, List<String> poklapanja, Set<String> tagovi) {}

    /**
     * Ponovo izračunava preporuke za trenutnog učesnika, briše stare i čuva nove,
     * pa vraća listu sortiranu po skoru poklapanja (opadajuće).
     */
    @Transactional
    public List<PreporukaDto> generisiMojePreporuke() {
        Korisnik korisnik = currentUserService.getCurrentKorisnik();
        if (!(korisnik instanceof Ucesnik ucesnik)) {
            throw new RuntimeException("Samo učesnici mogu imati preporuke.");
        }

        // Stare preporuke se uvek brišu pre ponovnog generisanja.
        preporukaRepository.deleteByUcesnik_KorisnikId(ucesnik.getKorisnikId());

        // Mapa: tag malim slovima -> originalni naziv interesovanja (za prikaz).
        Map<String, String> interesiLower = new HashMap<>();
        for (String i : ucesnik.getInteresi()) {
            if (i != null && !i.isBlank()) {
                interesiLower.putIfAbsent(i.trim().toLowerCase(), i.trim());
            }
        }
        if (interesiLower.isEmpty()) {
            return List.of();
        }

        // Događaji za koje je učesnik već registrovan se ne preporučuju ponovo.
        // Učitavamo samo ID-jeve (projekcija) da ne bismo punili persistence
        // context Dogadjaj entitetima bez njihovih tagova.
        Set<Long> registrovaniDogadjaji = new HashSet<>(
                registracijaRepository.findRegistrovaniDogadjajIds(ucesnik.getKorisnikId()));

        List<PreporukaPodaci> kandidati = new ArrayList<>();

        for (Dogadjaj d : dogadjajRepository.findAllWithLokacija()) {
            // Preporučuju se samo objavljeni i aktivni događaji.
            if (d.getStatus() != StatusDogadjaja.OBJAVLJEN && d.getStatus() != StatusDogadjaja.AKTIVAN) {
                continue;
            }
            if (registrovaniDogadjaji.contains(d.getDogadjajId())) {
                continue;
            }

            // Kopiramo tagove u običan Set dok je sesija otvorena (OSIV je isključen).
            Set<String> tagovi = new HashSet<>(d.getTagovi());
            if (tagovi.isEmpty()) {
                continue;
            }

            // Tagovi događaja koji se poklapaju sa interesovanjima učesnika.
            List<String> poklapanja = tagovi.stream()
                    .filter(t -> t != null && interesiLower.containsKey(t.trim().toLowerCase()))
                    .map(String::trim)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());

            if (poklapanja.isEmpty()) {
                continue;
            }

            double skor = (poklapanja.size() * 100.0) / tagovi.size();
            boolean znacajno = skor >= MIN_SKOR || poklapanja.size() >= MIN_POKLAPANJA;
            if (!znacajno) {
                continue;
            }

            Preporuka p = Preporuka.builder()
                    .ucesnik(ucesnik)
                    .dogadjaj(d)
                    .skorPoklapanja(BigDecimal.valueOf(skor).setScale(2, RoundingMode.HALF_UP))
                    .razlog(napraviRazlog(poklapanja, tagovi.size()))
                    .datumGenerisanja(LocalDate.now())
                    .build();
            kandidati.add(new PreporukaPodaci(p, poklapanja, tagovi));
        }

        preporukaRepository.saveAll(kandidati.stream().map(PreporukaPodaci::preporuka).toList());

        return kandidati.stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(PreporukaDto::getSkorPoklapanja).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Kontakti vezani za događaj: organizatori (zaposleni sa ulogom
     * KOORDINATOR_PROGRAMA) i registrovani učesnici (bez otkazanih registracija).
     */
    @Transactional(readOnly = true)
    public DogadjajKontaktiDto getKontaktiZaDogadjaj(Long dogadjajId) {
        List<KontaktKorisnikaDto> organizatori = zaposleniRepository
                .findByUloga(UlogaZaposlenog.KOORDINATOR_PROGRAMA)
                .stream()
                .map(z -> KontaktKorisnikaDto.builder()
                        .korisnikId(z.getKorisnikId())
                        .ime(z.getIme())
                        .prezime(z.getPrezime())
                        .email(z.getEmail())
                        .telefon(z.getTelefon())
                        .uloga(z.getUloga() != null ? z.getUloga().name() : null)
                        .pozicija(z.getPozicija())
                        .build())
                .sorted(Comparator.comparing(KontaktKorisnikaDto::getPrezime,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());

        // Distinktni učesnici sa aktivnom (neotkazanom) registracijom.
        Map<Long, KontaktKorisnikaDto> ucesniciMap = new LinkedHashMap<>();
        for (Registracija r : registracijaRepository.findByDogadjajIdWithDetails(dogadjajId)) {
            if (r.getStatus() == StatusRegistracije.OTKAZANA) {
                continue;
            }
            Ucesnik u = r.getUcesnik();
            ucesniciMap.computeIfAbsent(u.getKorisnikId(), k -> KontaktKorisnikaDto.builder()
                    .korisnikId(u.getKorisnikId())
                    .ime(u.getIme())
                    .prezime(u.getPrezime())
                    .email(u.getEmail())
                    .telefon(u.getTelefon())
                    .kompanija(u.getKompanija())
                    .pozicija(u.getPozicija())
                    .build());
        }
        List<KontaktKorisnikaDto> ucesnici = ucesniciMap.values().stream()
                .sorted(Comparator.comparing(KontaktKorisnikaDto::getPrezime,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());

        return DogadjajKontaktiDto.builder()
                .organizatori(organizatori)
                .ucesnici(ucesnici)
                .build();
    }

    private String napraviRazlog(List<String> poklapanja, int ukupnoTagova) {
        String temeTekst = String.join(", ", poklapanja);
        return String.format(
                "Poklapa se sa vašim interesovanjima: %s (%d od %d %s događaja).",
                temeTekst, poklapanja.size(), ukupnoTagova,
                ukupnoTagova == 1 ? "teme" : "tema");
    }

    /** Mapira u DTO koristeći već materijalizovane podatke (bez lazy pristupa). */
    private PreporukaDto toDto(PreporukaPodaci pd) {
        Preporuka p = pd.preporuka();
        Dogadjaj d = p.getDogadjaj();
        Lokacija l = d.getLokacija();

        return PreporukaDto.builder()
                .preporukaId(p.getPreporukaId())
                .skorPoklapanja(p.getSkorPoklapanja())
                .razlog(p.getRazlog())
                .datumGenerisanja(p.getDatumGenerisanja())
                .zajednickiTagovi(pd.poklapanja())
                .dogadjajId(d.getDogadjajId())
                .naziv(d.getNaziv())
                .datumPocetka(d.getDatumPocetka())
                .datumZavrsetka(d.getDatumZavrsetka())
                .opis(d.getOpis())
                .status(d.getStatus())
                .tagovi(pd.tagovi())
                .lokacijaNaziv(l != null ? l.getNaziv() : "")
                .lokacijaGrad(l != null ? l.getGrad() : "")
                .lokacijaDrzava(l != null ? l.getDrzava() : "")
                .build();
    }
}
