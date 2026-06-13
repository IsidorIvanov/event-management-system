package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.SesijaDetaljDto;
import com.eventsystem.event_management_system.dto.SesijaDto;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Govornik;
import com.eventsystem.event_management_system.model.Registracija;
import com.eventsystem.event_management_system.model.Sala;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.model.TipKarte;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.GovornikRepository;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.repository.SalaRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import com.eventsystem.event_management_system.utils.SesijaDtoMapper;
import com.eventsystem.event_management_system.utils.enums.StatusRegistracije;
import com.eventsystem.event_management_system.utils.enums.VrstaKarte;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
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

        return toDto(novaSesija);
    }

    @Transactional
    public SesijaDto updateSesija(Long id, SesijaDto dto) {
        Sesija existingSesija = sesijaRepository.findByIdWithGovornici(id)
                .orElseThrow(() -> new RuntimeException("Sesija not found with id: " + id));

        existingSesija.setNaziv(dto.getNaziv());
        existingSesija.setDatum(dto.getDatum());
        existingSesija.setVremePocetka(dto.getVremePocetka());
        existingSesija.setVremeZavrsetka(dto.getVremeZavrsetka());
        existingSesija.setTip(dto.getTip());
        existingSesija.setKapacitet(dto.getKapacitet());
        existingSesija.setOpis(dto.getOpis());

        sesijaRepository.save(existingSesija);

        return toDto(existingSesija);
    }

    public void deleteSesija(Long id) {
        if (!sesijaRepository.existsById(id)) {
            throw new RuntimeException("Sesija not found with id: " + id);
        }
        sesijaRepository.deleteById(id);
    }

    @Transactional
    public SesijaDto addGovornikToSesija(Long sesijaId, Long govornikId) {
        Sesija sesija = sesijaRepository.findByIdWithGovornici(sesijaId)
                .orElseThrow(() -> new RuntimeException("Sesija not found with id: " + sesijaId));

        Govornik govornik = govornikRepository.findById(govornikId)
                .orElseThrow(() -> new RuntimeException("Govornik not found with id: " + govornikId));

        sesija.getGovornici().add(govornik);
        sesijaRepository.save(sesija);

        return toDto(sesija);
    }

    @Transactional
    public SesijaDto removeGovornikFromSesija(Long sesijaId, Long govornikId) {
        Sesija sesija = sesijaRepository.findByIdWithGovornici(sesijaId)
                .orElseThrow(() -> new RuntimeException("Sesija not found with id: " + sesijaId));

        sesija.getGovornici().removeIf(g -> g.getGovornikId().equals(govornikId));
        sesijaRepository.save(sesija);

        return toDto(sesija);
    }
}
