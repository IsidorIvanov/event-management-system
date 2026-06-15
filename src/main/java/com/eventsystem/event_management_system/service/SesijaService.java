package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.EmailDetalj;
import com.eventsystem.event_management_system.dto.SesijaDetaljDto;
import com.eventsystem.event_management_system.dto.SesijaDto;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Govornik;
import com.eventsystem.event_management_system.model.Registracija;
import com.eventsystem.event_management_system.model.Sala;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.model.TipKarte;
import com.eventsystem.event_management_system.model.Ucesnik;
import com.eventsystem.event_management_system.model.UcesnikSesija;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.GovornikRepository;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.repository.SalaRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import com.eventsystem.event_management_system.repository.UcesnikSesijaRepository;
import com.eventsystem.event_management_system.utils.SesijaDtoMapper;
import com.eventsystem.event_management_system.utils.enums.KanalNotifikacije;
import com.eventsystem.event_management_system.utils.enums.StatusRegistracije;
import com.eventsystem.event_management_system.utils.enums.TipNotifikacije;
import com.eventsystem.event_management_system.utils.enums.VrstaKarte;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.eventsystem.event_management_system.utils.SesijaDtoMapper.toDto;

@Service
@RequiredArgsConstructor
public class SesijaService {

    private final SesijaRepository sesijaRepository;

    private final DogadjajRepository dogadjajRepository;

    private final SalaRepository salaRepository;

    private final GovornikRepository govornikRepository;

    private final RegistracijaRepository registracijaRepository;

    private final UcesnikSesijaRepository ucesnikSesijaRepository;

    private final NotifikacijaService notifikacijaService;

    private static final DateTimeFormatter SES_DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
    private static final DateTimeFormatter SES_VREME = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(readOnly = true)
    public List<SesijaDto> getSesijeByDogadjaj(Long dogadjajId) {
        if (!dogadjajRepository.existsById(dogadjajId)) {
            throw new RuntimeException("Dogadjaj not found with id: " + dogadjajId);
        }

        List<Sesija> sesije = sesijaRepository.findAllByDogadjaj_DogadjajId(dogadjajId);

        // Fetch only confirmed registrations for this event (for occupancy calculation)
        List<Registracija> potvrdjene = registracijaRepository
                .findByDogadjajIdWithDetails(dogadjajId)
                .stream()
                .filter(r -> r.getStatus() == StatusRegistracije.POTVRDJENA)
                .toList();

        return sesije.stream().map(s -> {
            SesijaDto dto = SesijaDtoMapper.toDto(s);

            long popunjenost = potvrdjene.stream().filter(r -> {
                TipKarte tk = r.getTipKarte();
                VrstaKarte vrsta = tk.getVrsta();
                String nazivTipa = tk.getId().getNazivTipa();

                return switch (vrsta) {
                    case VISEDNEVNA, BESPLATNA -> true;
                    case JEDNODNEVNA -> {
                        try {
                            LocalDate datumKarte = LocalDate.parse(nazivTipa);
                            yield s.getDatum().equals(datumKarte);
                        } catch (Exception e) {
                            yield false;
                        }
                    }
                    case POJEDINACNA_SESIJA -> s.getNaziv().equalsIgnoreCase(nazivTipa);
                };
            }).count();

            dto.setPopunjenost((int) popunjenost);
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SesijaDto getSesijaById(Long id) {
        Sesija sesija = sesijaRepository.findByIdWithGovornici(id)
                .orElseThrow(() -> new RuntimeException("Sesija not found with id: " + id));
        return toDto(sesija);
    }

    @Transactional(readOnly = true)
    public SesijaDetaljDto getSesijaDetalj(Long id) {
        Sesija sesija = sesijaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sesija nije pronadjena sa id: " + id));

        var dogadjaj = sesija.getDogadjaj();
        var lokacija = sesija.getSala().getLokacija();

        return SesijaDetaljDto.builder()
                .sesijaId(sesija.getSesijaId())
                .naziv(sesija.getNaziv())
                .datum(sesija.getDatum())
                .vremePocetka(sesija.getVremePocetka())
                .vremeZavrsetka(sesija.getVremeZavrsetka())
                .tip(sesija.getTip())
                .kapacitet(sesija.getKapacitet())
                .opis(sesija.getOpis())
                .nazivSale(sesija.getSala().getId().getNazivSale())
                .lokacijaId(lokacija.getLokacijaId())
                .lokacijaNaziv(lokacija.getNaziv())
                .dogadjajId(dogadjaj.getDogadjajId())
                .dogadjajNaziv(dogadjaj.getNaziv())
                .dogadjajStatus(dogadjaj.getStatus())
                .dogadjajDatumOd(dogadjaj.getDatumPocetka())
                .dogadjajDatumDo(dogadjaj.getDatumZavrsetka())
                .dogadjajOpis(dogadjaj.getOpis())
                .govornici(sesija.getGovornici().stream()
                        .map(g -> SesijaDetaljDto.GovornikPregledDto.builder()
                                .ime(g.getIme())
                                .prezime(g.getPrezime())
                                .kompanija(g.getKompanija())
                                .pozicija(g.getPozicija())
                                .build())
                        .toList())
                .build();
    }

    @Transactional
    public SesijaDto createSesija(SesijaDto dto) {
        Dogadjaj dogadjaj = dogadjajRepository.findById(dto.getDogadjajId())
                .orElseThrow(() -> new RuntimeException("Dogadjaj not found with id: " + dto.getDogadjajId()));

        Sala sala = salaRepository.findByLokacijaLokacijaIdAndIdNazivSale(dto.getLokacijaId(), dto.getNazivSale())
                .orElseThrow(() -> new RuntimeException("Sala not found with lokacijaId: " + dto.getLokacijaId() + " and nazivSale: " + dto.getNazivSale()));

        Sesija novaSesija = Sesija.builder()
                .dogadjaj(dogadjaj)
                .sala(sala)
                .naziv(dto.getNaziv())
                .datum(dto.getDatum())
                .vremePocetka(dto.getVremePocetka())
                .vremeZavrsetka(dto.getVremeZavrsetka())
                .tip(dto.getTip())
                .kapacitet(dto.getKapacitet())
                .opis(dto.getOpis())
                .build();

        sesijaRepository.save(novaSesija);

        // S1 — obavesti prijavljene učesnike događaja o novoj sesiji (PUSH).
        String sadrzaj = "Nova sesija \"" + novaSesija.getNaziv() + "\" je dodata na događaj \""
                + dogadjaj.getNaziv() + "\" (" + termin(novaSesija) + ").";
        for (Ucesnik ucesnik : ucesniciNaDogadjaju(dogadjaj.getDogadjajId())) {
            notifikacijaService.posalji(
                    ucesnik, TipNotifikacije.SESIJA, KanalNotifikacije.PUSH, sadrzaj, dogadjaj);
        }

        return toDto(novaSesija);
    }

    @Transactional
    public SesijaDto updateSesija(Long id, SesijaDto dto) {
        Sesija existingSesija = sesijaRepository.findByIdWithGovornici(id)
                .orElseThrow(() -> new RuntimeException("Sesija not found with id: " + id));

        // Zapamti stari termin radi detekcije izmene vremena (S2).
        boolean terminPromenjen = !dto.getDatum().equals(existingSesija.getDatum())
                || !dto.getVremePocetka().equals(existingSesija.getVremePocetka())
                || !dto.getVremeZavrsetka().equals(existingSesija.getVremeZavrsetka());

        existingSesija.setNaziv(dto.getNaziv());
        existingSesija.setDatum(dto.getDatum());
        existingSesija.setVremePocetka(dto.getVremePocetka());
        existingSesija.setVremeZavrsetka(dto.getVremeZavrsetka());
        existingSesija.setTip(dto.getTip());
        existingSesija.setKapacitet(dto.getKapacitet());
        existingSesija.setOpis(dto.getOpis());

        sesijaRepository.save(existingSesija);

        // S2 — ako je promenjen termin, obavesti učesnike koji imaju sesiju u rasporedu (PUSH + email).
        if (terminPromenjen) {
            Dogadjaj dogadjaj = existingSesija.getDogadjaj();
            String sala = existingSesija.getSala().getId().getNazivSale();
            String sadrzaj = "Sesija \"" + existingSesija.getNaziv() + "\" iz vašeg rasporeda je izmenjena. "
                    + "Novi termin: " + termin(existingSesija) + " (sala " + sala + ").";
            String emailPoruka = "sesija \"" + existingSesija.getNaziv()
                    + "\" iz vašeg rasporeda je izmenjena. Ažurirani podaci su u nastavku.";
            String emailNaslov = "Izmena sesije - " + existingSesija.getNaziv();
            List<EmailDetalj> detalji = List.of(
                    new EmailDetalj("Sesija", existingSesija.getNaziv()),
                    new EmailDetalj("Događaj", dogadjaj.getNaziv()),
                    new EmailDetalj("Novi termin", termin(existingSesija)),
                    new EmailDetalj("Sala", sala));
            for (Ucesnik ucesnik : ucesniciURasporedu(existingSesija.getSesijaId())) {
                notifikacijaService.posaljiSaEmailom(
                        ucesnik, TipNotifikacije.SESIJA, sadrzaj, dogadjaj, emailNaslov, emailPoruka, detalji);
            }
        }

        return toDto(existingSesija);
    }

    @Transactional
    public void deleteSesija(Long id) {
        Sesija sesija = sesijaRepository.findByIdWithGovornici(id)
                .orElseThrow(() -> new RuntimeException("Sesija not found with id: " + id));

        Dogadjaj dogadjaj = sesija.getDogadjaj();
        String naziv = sesija.getNaziv();

        // Učesnici sa sesijom u rasporedu — zapamti pre brisanja (S3).
        List<UcesnikSesija> uRasporedu = ucesnikSesijaRepository.findBySesijaIdWithUcesnik(id);
        List<Ucesnik> primaoci = new ArrayList<>();
        Set<Long> vidjeni = new HashSet<>();
        for (UcesnikSesija us : uRasporedu) {
            Ucesnik u = us.getUcesnik();
            if (vidjeni.add(u.getKorisnikId())) primaoci.add(u);
        }

        // Ukloni stavke rasporeda (FK na sesiju), pa obriši sesiju.
        ucesnikSesijaRepository.deleteAll(uRasporedu);
        sesijaRepository.delete(sesija);

        // S3 — obavesti učesnike da je sesija otkazana (PUSH + email).
        String sadrzaj = "Sesija \"" + naziv + "\" sa događaja \"" + dogadjaj.getNaziv()
                + "\" je otkazana i uklonjena iz vašeg rasporeda.";
        String emailPoruka = "sesija \"" + naziv + "\" sa događaja \"" + dogadjaj.getNaziv()
                + "\" je otkazana i više se neće održati.";
        String emailNaslov = "Otkazana sesija - " + naziv;
        List<EmailDetalj> detalji = List.of(
                new EmailDetalj("Sesija", naziv),
                new EmailDetalj("Događaj", dogadjaj.getNaziv()));
        for (Ucesnik ucesnik : primaoci) {
            notifikacijaService.posaljiSaEmailom(
                    ucesnik, TipNotifikacije.SESIJA, sadrzaj, dogadjaj, emailNaslov, emailPoruka, detalji);
        }
    }

    @Transactional
    public SesijaDto addGovornikToSesija(Long sesijaId, Long govornikId) {
        Sesija sesija = sesijaRepository.findByIdWithGovornici(sesijaId)
                .orElseThrow(() -> new RuntimeException("Sesija not found with id: " + sesijaId));

        Govornik govornik = govornikRepository.findById(govornikId)
                .orElseThrow(() -> new RuntimeException("Govornik not found with id: " + govornikId));

        boolean dodat = sesija.getGovornici().add(govornik);
        sesijaRepository.save(sesija);

        // S4 — obavesti učesnike sa sesijom u rasporedu o novom govorniku (PUSH).
        if (dodat) {
            obavestiOPromeniGovornika(sesija, govornik.getIme() + " " + govornik.getPrezime()
                    + " je dodat/a kao govornik na sesiji \"" + sesija.getNaziv() + "\".");
        }

        return toDto(sesija);
    }

    @Transactional
    public SesijaDto removeGovornikFromSesija(Long sesijaId, Long govornikId) {
        Sesija sesija = sesijaRepository.findByIdWithGovornici(sesijaId)
                .orElseThrow(() -> new RuntimeException("Sesija not found with id: " + sesijaId));

        Govornik uklonjen = sesija.getGovornici().stream()
                .filter(g -> g.getGovornikId().equals(govornikId))
                .findFirst()
                .orElse(null);
        sesija.getGovornici().removeIf(g -> g.getGovornikId().equals(govornikId));
        sesijaRepository.save(sesija);

        // S4 — obavesti učesnike sa sesijom u rasporedu o uklanjanju govornika (PUSH).
        if (uklonjen != null) {
            obavestiOPromeniGovornika(sesija, uklonjen.getIme() + " " + uklonjen.getPrezime()
                    + " više nije govornik na sesiji \"" + sesija.getNaziv() + "\".");
        }

        return toDto(sesija);
    }

    private void obavestiOPromeniGovornika(Sesija sesija, String sadrzaj) {
        Dogadjaj dogadjaj = sesija.getDogadjaj();
        for (Ucesnik ucesnik : ucesniciURasporedu(sesija.getSesijaId())) {
            notifikacijaService.posalji(
                    ucesnik, TipNotifikacije.SESIJA, KanalNotifikacije.PUSH, sadrzaj, dogadjaj);
        }
    }

    /** Distinct učesnici koji imaju sesiju u svom rasporedu (S2/S3/S4). */
    private List<Ucesnik> ucesniciURasporedu(Long sesijaId) {
        List<Ucesnik> rezultat = new ArrayList<>();
        Set<Long> vidjeni = new HashSet<>();
        for (UcesnikSesija us : ucesnikSesijaRepository.findBySesijaIdWithUcesnik(sesijaId)) {
            Ucesnik u = us.getUcesnik();
            if (vidjeni.add(u.getKorisnikId())) rezultat.add(u);
        }
        return rezultat;
    }

    /** Distinct potvrđeni učesnici prijavljeni na događaj (S1). */
    private List<Ucesnik> ucesniciNaDogadjaju(Long dogadjajId) {
        List<Ucesnik> rezultat = new ArrayList<>();
        Set<Long> vidjeni = new HashSet<>();
        for (Registracija r : registracijaRepository.findByDogadjajIdWithDetails(dogadjajId)) {
            if (r.getStatus() != StatusRegistracije.POTVRDJENA) continue;
            Ucesnik u = r.getUcesnik();
            if (vidjeni.add(u.getKorisnikId())) rezultat.add(u);
        }
        return rezultat;
    }

    private String termin(Sesija s) {
        return s.getDatum().format(SES_DATUM) + " "
                + s.getVremePocetka().format(SES_VREME) + "–" + s.getVremeZavrsetka().format(SES_VREME);
    }
}
