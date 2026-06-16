package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.SredstvaDogadjajaDto;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Nabavka;
import com.eventsystem.event_management_system.model.Sesija;
import com.eventsystem.event_management_system.model.StavkaNabavke;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.NabavkaRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SredstvaDogadjajaService {

    private final DogadjajRepository dogadjajRepository;
    private final SesijaRepository sesijaRepository;
    private final NabavkaRepository nabavkaRepository;
    private final DodelaOpremeService dodelaOpremeService;
    private final InventarPotrebeService inventarPotrebeService;

    @Transactional(readOnly = true)
    public SredstvaDogadjajaDto getSredstva(Long dogadjajId) {
        Dogadjaj dogadjaj = dogadjajRepository.findById(dogadjajId)
                .orElseThrow(() -> new NotFoundException("Događaj nije pronađen: " + dogadjajId));

        List<Sesija> sesije = sesijaRepository.findAllByDogadjaj_DogadjajId(dogadjajId);
        List<SredstvaDogadjajaDto.SalaSredstvoDto> sale = sesije.stream()
                .map(s -> SredstvaDogadjajaDto.SalaSredstvoDto.builder()
                        .nazivSale(s.getSala() != null && s.getSala().getId() != null
                                ? s.getSala().getId().getNazivSale() : null)
                        .lokacijaNaziv(s.getSala() != null && s.getSala().getLokacija() != null
                                ? s.getSala().getLokacija().getNaziv() : null)
                        .sesijaId(s.getSesijaId())
                        .sesijaNaziv(s.getNaziv())
                        .datum(s.getDatum() != null ? s.getDatum().toString() : null)
                        .build())
                .toList();

        List<Nabavka> nabavke = nabavkaRepository.findByDogadjajWithStavke(dogadjajId);
        List<SredstvaDogadjajaDto.NabavkaSredstvoDto> nabavkeDto = nabavke.stream()
                .map(n -> SredstvaDogadjajaDto.NabavkaSredstvoDto.builder()
                        .nabavkaId(n.getNabavkaId())
                        .status(n.getStatus() != null ? n.getStatus().name() : null)
                        .dobavljacNaziv(n.getDobavljac() != null ? n.getDobavljac().getNaziv() : null)
                        .stavkeOpis(opisStavki(n))
                        .build())
                .toList();

        var validacija = inventarPotrebeService.validirajPotrebeInventarom(dogadjajId);

        return SredstvaDogadjajaDto.builder()
                .dogadjajId(dogadjajId)
                .dogadjajNaziv(dogadjaj.getNaziv())
                .sale(sale)
                .nabavke(nabavkeDto)
                .dodeleOpreme(dodelaOpremeService.getByDogadjaj(dogadjajId))
                .upozorenja(validacija.getUpozorenja())
                .build();
    }

    private String opisStavki(Nabavka nabavka) {
        if (nabavka.getStavke() == null || nabavka.getStavke().isEmpty()) {
            return "—";
        }
        Set<String> nazivi = new LinkedHashSet<>();
        for (StavkaNabavke s : nabavka.getStavke()) {
            nazivi.add(s.getNazivResursa() + " x" + s.getKolicina());
        }
        return nazivi.stream().collect(Collectors.joining(", "));
    }
}
