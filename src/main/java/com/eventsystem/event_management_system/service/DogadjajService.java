package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DogadjajDto;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Lokacija;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.LokacijaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DogadjajService {

    private final DogadjajRepository dogadjajRepository;

    private final LokacijaRepository lokacijaRepository;

    public DogadjajDto saveDogadjaj(DogadjajDto dto) {
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

        return dto;
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
}
