package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.TipKarteDto;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.TipKarte;
import com.eventsystem.event_management_system.model.compositePK.TipKarteId;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.TipKarteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipKarteService {

    private final TipKarteRepository tipKarteRepository;

    private final DogadjajRepository dogadjajRepository;

    public TipKarteDto createTipKarte(TipKarteDto dto) {
        Dogadjaj dogadjaj = dogadjajRepository.findById(dto.getDogadjajId())
                .orElseThrow(() -> new RuntimeException("Događaj nije pronađen sa id: " + dto.getDogadjajId()));

        if (tipKarteRepository.existsByDogadjaj_DogadjajIdAndId_NazivTipa(dto.getDogadjajId(), dto.getNazivTipa())) {
            throw new RuntimeException("Tip karte '" + dto.getNazivTipa() + "' već postoji za ovaj događaj.");
        }

        validateZbirKvota(dogadjaj, dto.getNazivTipa(), dto.getKvota());

        TipKarteId id = new TipKarteId(dto.getDogadjajId(), dto.getNazivTipa());

        TipKarte tipKarte = TipKarte.builder()
                .id(id)
                .dogadjaj(dogadjaj)
                .vrsta(dto.getVrsta())
                .cena(dto.getCena())
                .kvota(dto.getKvota())
                .opis(dto.getOpis())
                .build();

        tipKarteRepository.save(tipKarte);
        return dto;
    }

    public List<TipKarteDto> getTipKarteByDogadjaj(Long dogadjajId) {
        return tipKarteRepository.findAllByDogadjaj_DogadjajId(dogadjajId)
                .stream()
                .map(t -> TipKarteDto.builder()
                        .dogadjajId(t.getDogadjaj().getDogadjajId())
                        .nazivTipa(t.getId().getNazivTipa())
                        .vrsta(t.getVrsta())
                        .cena(t.getCena())
                        .kvota(t.getKvota())
                        .opis(t.getOpis())
                        .build())
                .collect(Collectors.toList());
    }

    public TipKarteDto updateTipKarte(Long dogadjajId, String nazivTipa, TipKarteDto dto) {
        TipKarteId id = new TipKarteId(dogadjajId, nazivTipa);
        TipKarte existing = tipKarteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tip karte nije pronađen."));

        Dogadjaj dogadjaj = dogadjajRepository.findById(dogadjajId)
                .orElseThrow(() -> new RuntimeException("Događaj nije pronađen sa id: " + dogadjajId));

        validateZbirKvota(dogadjaj, nazivTipa, dto.getKvota());

        existing.setVrsta(dto.getVrsta());
        existing.setCena(dto.getCena());
        existing.setKvota(dto.getKvota());
        existing.setOpis(dto.getOpis());

        tipKarteRepository.save(existing);
        return dto;
    }

    /**
     * Proverava da li zbir kvota svih tipova karata događaja (uz novu/izmenjenu
     * vrednost za {@code nazivTipa}) prelazi maksimalni kapacitet događaja.
     */
    private void validateZbirKvota(Dogadjaj dogadjaj, String nazivTipa, int novaKvota) {
        int zbirOstalih = tipKarteRepository.findAllByDogadjaj_DogadjajId(dogadjaj.getDogadjajId())
                .stream()
                .filter(t -> !t.getId().getNazivTipa().equals(nazivTipa))
                .mapToInt(TipKarte::getKvota)
                .sum();

        int ukupno = zbirOstalih + novaKvota;
        if (ukupno > dogadjaj.getMaksKapacitet()) {
            throw new RuntimeException("Zbir kvota tipova karata (" + ukupno
                    + ") ne sme biti veći od maksimalnog kapaciteta događaja ("
                    + dogadjaj.getMaksKapacitet() + ").");
        }
    }

    public void deleteTipKarte(Long dogadjajId, String nazivTipa) {
        TipKarteId id = new TipKarteId(dogadjajId, nazivTipa);
        if (!tipKarteRepository.existsById(id)) {
            throw new RuntimeException("Tip karte nije pronađen.");
        }
        tipKarteRepository.deleteById(id);
    }
}
