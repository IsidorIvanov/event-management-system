package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.SesijaDto;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Sala;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.SalaRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SesijaService {

    private final SesijaRepository sesijaRepository;

    private final DogadjajRepository dogadjajRepository;

    private final SalaRepository salaRepository;

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

        return dto;
    }

    public SesijaDto updateSesija(Long id, SesijaDto dto) {
        Sesija existingSesija = sesijaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sesija not found with id: " + id));

        existingSesija.setNaziv(dto.getNaziv());
        existingSesija.setDatum(dto.getDatum());
        existingSesija.setVremePocetka(dto.getVremePocetka());
        existingSesija.setVremeZavrsetka(dto.getVremeZavrsetka());
        existingSesija.setTip(dto.getTip());
        existingSesija.setKapacitet(dto.getKapacitet());
        existingSesija.setOpis(dto.getOpis());

        sesijaRepository.save(existingSesija);

        return dto;
    }

    public void deleteSesija(Long id) {
        if (!sesijaRepository.existsById(id)) {
            throw new RuntimeException("Sesija not found with id: " + id);
        }
        sesijaRepository.deleteById(id);
    }
}
