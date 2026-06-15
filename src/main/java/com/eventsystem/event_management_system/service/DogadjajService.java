package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DogadjajDto;
import com.eventsystem.event_management_system.dto.DogadjajResponseDto;
import com.eventsystem.event_management_system.dto.EmailDetalj;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.model.Registracija;
import com.eventsystem.event_management_system.model.Ucesnik;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import com.eventsystem.event_management_system.utils.enums.StatusRegistracije;
import com.eventsystem.event_management_system.utils.enums.TipNotifikacije;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DogadjajService {

    private final DogadjajRepository dogadjajRepository;

    private final LokacijaRepository lokacijaRepository;

    private final RegistracijaRepository registracijaRepository;

    private final NotifikacijaService notifikacijaService;

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    @Transactional
    public DogadjajResponseDto saveDogadjaj(DogadjajDto dto) {
        Lokacija lokacija = lokacijaRepository.findById(dto.getLokacijaId())
                .orElseThrow(() -> new RuntimeException("Lokacija not found with id: " + dto.getLokacijaId()));

        Dogadjaj dogadjaj = Dogadjaj.builder()
                .lokacija(lokacija)
                .naziv(dto.getNaziv())
                .datumPocetka(LocalDate.parse(dto.getDatumPocetka()))
                .datumZavrsetka(LocalDate.parse(dto.getDatumZavrsetka()))
                .maksKapacitet(dto.getMaksKapacitet())
                .opis(dto.getOpis())
                .build();

        dogadjaj.setTagovi(normalizujTagove(dto.getTagovi()));

        dogadjajRepository.save(dogadjaj);

        return toResponseDto(dogadjaj);
    }

    @Transactional
    public DogadjajResponseDto updateDogadjaj(Long id, DogadjajDto dto) {
        Lokacija lokacija = lokacijaRepository.findById(dto.getLokacijaId())
                .orElseThrow(() -> new RuntimeException("Lokacija not found with id: " + dto.getLokacijaId()));

        // findByIdWithLokacija fetch-uje i tagove, pa je kolekcija inicijalizovana
        // (OSIV je isključen, pa lazy pristup van transakcije baca grešku).
        Dogadjaj existingDogadjaj = dogadjajRepository.findByIdWithLokacija(id)
                .orElseThrow(() -> new RuntimeException("Dogadjaj not found with id: " + id));

        // Zapamti stare vrednosti radi detekcije izmene datuma/lokacije (D5).
        LocalDate stariPocetak = existingDogadjaj.getDatumPocetka();
        LocalDate stariZavrsetak = existingDogadjaj.getDatumZavrsetka();
        Lokacija staraLokacija = existingDogadjaj.getLokacija();

        existingDogadjaj.setLokacija(lokacija);
        existingDogadjaj.setNaziv(dto.getNaziv());
        existingDogadjaj.setDatumPocetka(LocalDate.parse(dto.getDatumPocetka()));
        existingDogadjaj.setDatumZavrsetka(LocalDate.parse(dto.getDatumZavrsetka()));
        existingDogadjaj.setMaksKapacitet(dto.getMaksKapacitet());
        existingDogadjaj.setOpis(dto.getOpis());

        existingDogadjaj.getTagovi().clear();
        existingDogadjaj.getTagovi().addAll(normalizujTagove(dto.getTagovi()));

        dogadjajRepository.save(existingDogadjaj);

        obavestiUcesnikeOIzmeni(existingDogadjaj, stariPocetak, stariZavrsetak, staraLokacija);

        return toResponseDto(existingDogadjaj);
    }

    /**
     * D5 — kada se promeni datum ili lokacija događaja, obaveštava sve prijavljene
     * učesnike (PUSH + email) o tome šta se konkretno promenilo. Kapacitet i opis
     * se ne računaju kao izmena relevantna za učesnika.
     */
    private void obavestiUcesnikeOIzmeni(Dogadjaj dogadjaj, LocalDate stariPocetak,
                                         LocalDate stariZavrsetak, Lokacija staraLokacija) {
        boolean datumPromenjen = !dogadjaj.getDatumPocetka().equals(stariPocetak)
                || !dogadjaj.getDatumZavrsetka().equals(stariZavrsetak);
        Lokacija novaLokacija = dogadjaj.getLokacija();
        Long staraLokId = staraLokacija != null ? staraLokacija.getLokacijaId() : null;
        Long novaLokId = novaLokacija != null ? novaLokacija.getLokacijaId() : null;
        boolean lokacijaPromenjena = staraLokId == null
                ? novaLokId != null : !staraLokId.equals(novaLokId);

        if (!datumPromenjen && !lokacijaPromenjena) {
            return;
        }

        List<EmailDetalj> detalji = new ArrayList<>();
        detalji.add(new EmailDetalj("Događaj", dogadjaj.getNaziv()));

        // PUSH/zvonce nosi poruku sa zasebnim, labeliranim rečenicama (bez "; ");
        // email koristi kratak uvod + karticu sa vrednostima.
        StringBuilder tekst = new StringBuilder("Došlo je do izmene na događaju \"")
                .append(dogadjaj.getNaziv()).append("\".");
        if (datumPromenjen) {
            String period = formatirajPeriod(dogadjaj.getDatumPocetka(), dogadjaj.getDatumZavrsetka());
            tekst.append(" Novi datum: ").append(period);
            detalji.add(new EmailDetalj("Novi datum", period));
        }
        if (lokacijaPromenjena) {
            String opis = opisLokacije(novaLokacija);
            tekst.append(" Nova lokacija: ").append(opis).append(".");
            detalji.add(new EmailDetalj("Nova lokacija", opis));
        }
        String sadrzaj = tekst.toString();
        String emailPoruka = "obaveštavamo Vas da je došlo do izmene na događaju \""
                + dogadjaj.getNaziv() + "\" za koji ste prijavljeni. Ažurirani podaci su u nastavku.";
        String emailNaslov = "Izmena događaja - " + dogadjaj.getNaziv();

        Set<Long> obavesteni = new HashSet<>();
        for (Registracija r : registracijaRepository.findByDogadjajIdWithDetails(dogadjaj.getDogadjajId())) {
            if (r.getStatus() == StatusRegistracije.OTKAZANA) {
                continue;
            }
            Ucesnik ucesnik = r.getUcesnik();
            if (!obavesteni.add(ucesnik.getKorisnikId())) {
                continue; // isti učesnik je već obavešten
            }
            notifikacijaService.posaljiSaEmailom(
                    ucesnik, TipNotifikacije.DOGADJAJ, sadrzaj, dogadjaj, emailNaslov, emailPoruka, detalji);
        }
    }

    private String formatirajPeriod(LocalDate pocetak, LocalDate zavrsetak) {
        String p = pocetak.format(DATUM_FORMAT);
        if (zavrsetak == null || zavrsetak.equals(pocetak)) {
            return p;
        }
        return p + " – " + zavrsetak.format(DATUM_FORMAT);
    }

    private String opisLokacije(Lokacija l) {
        if (l == null) {
            return "—";
        }
        List<String> delovi = new ArrayList<>();
        if (l.getNaziv() != null && !l.getNaziv().isBlank()) delovi.add(l.getNaziv());
        if (l.getGrad() != null && !l.getGrad().isBlank()) delovi.add(l.getGrad());
        if (l.getDrzava() != null && !l.getDrzava().isBlank()) delovi.add(l.getDrzava());
        return delovi.isEmpty() ? "—" : String.join(", ", delovi);
    }

    public void deleteDogadjaj(Long id) {
        if (!dogadjajRepository.existsById(id)) {
            throw new RuntimeException("Dogadjaj not found with id: " + id);
        }
        dogadjajRepository.deleteById(id);
    }

    /**
     * D6 — prebacuje događaje koji su počeli (status OBJAVLJEN, a datum početka je
     * stigao i događaj još traje) u status AKTIVAN i obaveštava prijavljene
     * učesnike (PUSH). Poziva se iz zakazanog posla.
     *
     * @return broj aktiviranih događaja
     */
    @Transactional
    public int aktivirajZapoceteDogadjaje() {
        List<Dogadjaj> zaAktivaciju =
                dogadjajRepository.findZaAktivaciju(StatusDogadjaja.OBJAVLJEN, LocalDate.now());
        for (Dogadjaj dogadjaj : zaAktivaciju) {
            dogadjaj.setStatus(StatusDogadjaja.AKTIVAN);
            obavestiOPocetku(dogadjaj);
        }
        return zaAktivaciju.size();
    }

    /**
     * D7 — prebacuje događaje kojima je prošao datum završetka (status OBJAVLJEN
     * ili AKTIVAN) u status ZAVRSEN i šalje potvrđenim učesnicima zahvalnicu sa
     * molbom za utiske (EMAIL). Poziva se iz zakazanog posla.
     *
     * @return broj završenih događaja
     */
    @Transactional
    public int zavrsiDogadjaje() {
        List<Dogadjaj> zaZavrsetak = dogadjajRepository.findZaZavrsetak(
                List.of(StatusDogadjaja.OBJAVLJEN, StatusDogadjaja.AKTIVAN), LocalDate.now());
        for (Dogadjaj dogadjaj : zaZavrsetak) {
            dogadjaj.setStatus(StatusDogadjaja.ZAVRSEN);
            obavestiOZavrsetku(dogadjaj);
        }
        return zaZavrsetak.size();
    }

    /** Šalje potvrđenim učesnicima zahvalnicu i molbu za utiske (D7, samo email). */
    private void obavestiOZavrsetku(Dogadjaj dogadjaj) {
        String poruka = "hvala Vam što ste prisustvovali događaju \"" + dogadjaj.getNaziv()
                + "\". Nadamo se da ste uživali! Bićemo zahvalni ako podelite svoje utiske i "
                + "predloge — pomažu nam da budući događaji budu još bolji.";
        String emailNaslov = "Hvala na učešću - " + dogadjaj.getNaziv();
        Set<Long> obavesteni = new HashSet<>();
        for (Registracija r : registracijaRepository.findByDogadjajIdWithDetails(dogadjaj.getDogadjajId())) {
            if (r.getStatus() != StatusRegistracije.POTVRDJENA) {
                continue; // zahvaljujemo se samo onima koji su zaista učestvovali
            }
            Ucesnik ucesnik = r.getUcesnik();
            if (!obavesteni.add(ucesnik.getKorisnikId())) {
                continue;
            }
            notifikacijaService.posaljiEmail(
                    ucesnik, TipNotifikacije.DOGADJAJ, poruka, dogadjaj, emailNaslov);
        }
    }

    /** Obaveštava potvrđene učesnike da je događaj počeo (D6, PUSH). */
    private void obavestiOPocetku(Dogadjaj dogadjaj) {
        String sadrzaj = "Događaj \"" + dogadjaj.getNaziv()
                + "\" je počeo. Želimo Vam prijatno iskustvo!";
        Set<Long> obavesteni = new HashSet<>();
        for (Registracija r : registracijaRepository.findByDogadjajIdWithDetails(dogadjaj.getDogadjajId())) {
            if (r.getStatus() != StatusRegistracije.POTVRDJENA) {
                continue; // obaveštavamo samo potvrđene učesnike (ne i listu čekanja)
            }
            Ucesnik ucesnik = r.getUcesnik();
            if (!obavesteni.add(ucesnik.getKorisnikId())) {
                continue;
            }
            notifikacijaService.posalji(
                    ucesnik, TipNotifikacije.DOGADJAJ, KanalNotifikacije.PUSH, sadrzaj, dogadjaj);
        }
    }

    @Transactional(readOnly = true)
    public List<DogadjajResponseDto> getAllDogadjaji() {
        return dogadjajRepository.findAllWithLokacija().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DogadjajResponseDto getDogadjajById(Long id) {
        Dogadjaj d = dogadjajRepository.findByIdWithLokacija(id)
                .orElseThrow(() -> new RuntimeException("Dogadjaj not found with id: " + id));
        return toResponseDto(d);
    }

    private DogadjajResponseDto toResponseDto(Dogadjaj d) {
        Lokacija l = d.getLokacija();
        return new DogadjajResponseDto(
                d.getDogadjajId(),
                d.getNaziv(),
                d.getDatumPocetka().toString(),
                d.getDatumZavrsetka().toString(),
                d.getMaksKapacitet(),
                d.getOpis(),
                d.getStatus(),
                l != null ? l.getNaziv() : "",
                l != null ? l.getGrad() : "",
                l != null ? l.getDrzava() : "",
                l != null ? l.getAdresa() : "",
                new HashSet<>(d.getTagovi())
        );
    }

    /**
     * Čisti listu tagova: trimuje, izbacuje prazne i uklanja duplikate
     * (bez obzira na velika/mala slova), čuvajući originalni unos.
     */
    private Set<String> normalizujTagove(List<String> tagovi) {
        Set<String> rezultat = new LinkedHashSet<>();
        if (tagovi == null) {
            return rezultat;
        }
        Set<String> vidjeni = new HashSet<>();
        for (String tag : tagovi) {
            if (tag == null) {
                continue;
            }
            String ocisceno = tag.trim();
            if (!ocisceno.isEmpty() && vidjeni.add(ocisceno.toLowerCase())) {
                rezultat.add(ocisceno);
            }
        }
        return rezultat;
    }
}
