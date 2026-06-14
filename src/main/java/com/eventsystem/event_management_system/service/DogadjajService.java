package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DogadjajDto;
import com.eventsystem.event_management_system.dto.DogadjajResponseDto;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

        existingDogadjaj.setLokacija(lokacija);
        existingDogadjaj.setNaziv(dto.getNaziv());
        existingDogadjaj.setDatumPocetka(LocalDate.parse(dto.getDatumPocetka()));
        existingDogadjaj.setDatumZavrsetka(LocalDate.parse(dto.getDatumZavrsetka()));
        existingDogadjaj.setMaksKapacitet(dto.getMaksKapacitet());
        existingDogadjaj.setOpis(dto.getOpis());

        existingDogadjaj.getTagovi().clear();
        existingDogadjaj.getTagovi().addAll(normalizujTagove(dto.getTagovi()));

        dogadjajRepository.save(existingDogadjaj);

        return toResponseDto(existingDogadjaj);
    }

    public void deleteDogadjaj(Long id) {
        if (!dogadjajRepository.existsById(id)) {
            throw new RuntimeException("Dogadjaj not found with id: " + id);
        }
        dogadjajRepository.deleteById(id);
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
