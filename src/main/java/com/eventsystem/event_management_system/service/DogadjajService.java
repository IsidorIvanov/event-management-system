package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DogadjajDto;
import com.eventsystem.event_management_system.dto.DogadjajResponseDto;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import com.eventsystem.event_management_system.utils.DogadjajDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DogadjajService {

    private final DogadjajRepository dogadjajRepository;

    private final LokacijaRepository lokacijaRepository;

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

        dogadjajRepository.save(dogadjaj);

        return toResponseDto(dogadjaj);
    }

    public DogadjajDto updateDogadjaj(Long id, DogadjajDto dto) {
        Lokacija lokacija = lokacijaRepository.findById(dto.getLokacijaId())
                .orElseThrow(() -> new RuntimeException("Lokacija not found with id: " + dto.getLokacijaId()));

        Dogadjaj existingDogadjaj = dogadjajRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dogadjaj not found with id: " + id));

        existingDogadjaj.setLokacija(lokacija);
        existingDogadjaj.setNaziv(dto.getNaziv());
        existingDogadjaj.setDatumPocetka(LocalDate.parse(dto.getDatumPocetka()));
        existingDogadjaj.setDatumZavrsetka(LocalDate.parse(dto.getDatumZavrsetka()));
        existingDogadjaj.setMaksKapacitet(dto.getMaksKapacitet());
        existingDogadjaj.setOpis(dto.getOpis());

        dogadjajRepository.save(existingDogadjaj);

        return dto;
    }

    public void deleteDogadjaj(Long id) {
        if (!dogadjajRepository.existsById(id)) {
            throw new RuntimeException("Dogadjaj not found with id: " + id);
        }
        dogadjajRepository.deleteById(id);
    }

    public List<DogadjajResponseDto> getAllDogadjaji() {
        return dogadjajRepository.findAllWithLokacija().stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
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
                l != null ? l.getDrzava() : ""
        );
    }
}
