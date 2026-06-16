package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DodelaOpremeDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.*;
import com.eventsystem.event_management_system.repository.DodelaOpremeRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.InventarKretanjeRepository;
import com.eventsystem.event_management_system.repository.SesijaRepository;
import com.eventsystem.event_management_system.utils.enums.StatusDodeleOpreme;
import com.eventsystem.event_management_system.utils.enums.TipInventarKretanja;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DodelaOpremeService {

    private final DodelaOpremeRepository dodelaOpremeRepository;
    private final OpremaService opremaService;
    private final DogadjajRepository dogadjajRepository;
    private final SesijaRepository sesijaRepository;
    private final InventarKretanjeRepository inventarKretanjeRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<DodelaOpremeDto> getByDogadjaj(Long dogadjajId) {
        return dodelaOpremeRepository.findByDogadjajWithDetalji(dogadjajId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DodelaOpremeDto getById(Long id) {
        return toDto(findEntity(id));
    }

    @Transactional
    public DodelaOpremeDto rezervisi(DodelaOpremeDto dto) {
        validatePeriod(dto.getDatumOd(), dto.getDatumDo());
        Oprema oprema = opremaService.findEntity(dto.getOpremaId());
        Dogadjaj dogadjaj = dogadjajRepository.findById(dto.getDogadjajId())
                .orElseThrow(() -> new NotFoundException("Događaj nije pronađen: " + dto.getDogadjajId()));
        Sesija sesija = resolveSesija(dto.getSesijaId(), dogadjaj.getDogadjajId());

        opremaService.rezervisi(oprema, dto.getKolicina());

        DodelaOpreme dodela = DodelaOpreme.builder()
                .oprema(oprema)
                .dogadjaj(dogadjaj)
                .sesija(sesija)
                .kolicina(dto.getKolicina())
                .datumOd(dto.getDatumOd())
                .datumDo(dto.getDatumDo())
                .status(StatusDodeleOpreme.REZERVISANO)
                .napomena(dto.getNapomena())
                .build();
        dodela = dodelaOpremeRepository.save(dodela);

        zabeleziKretanje(oprema, dodela, TipInventarKretanja.REZERVACIJA, dto.getKolicina(),
                "Rezervacija za događaj " + dogadjaj.getNaziv());

        return toDto(dodela);
    }

    @Transactional
    public DodelaOpremeDto aktiviraj(Long dodelaId) {
        DodelaOpreme dodela = findEntity(dodelaId);
        if (dodela.getStatus() != StatusDodeleOpreme.REZERVISANO) {
            throw new BadRequestException("Samo rezervisana dodela može biti aktivirana.");
        }
        opremaService.aktivirajRezervaciju(dodela.getOprema(), dodela.getKolicina());
        dodela.setStatus(StatusDodeleOpreme.NA_DOGADJAJU);
        return toDto(dodelaOpremeRepository.save(dodela));
    }

    @Transactional
    public DodelaOpremeDto oslobodi(Long dodelaId) {
        DodelaOpreme dodela = findEntity(dodelaId);
        if (dodela.getStatus() == StatusDodeleOpreme.ZAVRSENO || dodela.getStatus() == StatusDodeleOpreme.OTKAZANO) {
            throw new BadRequestException("Dodela je već završena ili otkazana.");
        }

        Oprema oprema = dodela.getOprema();
        if (dodela.getStatus() == StatusDodeleOpreme.NA_DOGADJAJU) {
            opremaService.oslobodiSaDogadjaja(oprema, dodela.getKolicina());
        } else {
            opremaService.oslobodiRezervaciju(oprema, dodela.getKolicina());
        }

        dodela.setStatus(StatusDodeleOpreme.ZAVRSENO);
        dodela = dodelaOpremeRepository.save(dodela);

        zabeleziKretanje(oprema, dodela, TipInventarKretanja.OSLOBODJENJE, dodela.getKolicina(),
                "Oslobađanje opreme sa događaja");

        return toDto(dodela);
    }

    @Transactional
    public DodelaOpremeDto otkazi(Long dodelaId) {
        DodelaOpreme dodela = findEntity(dodelaId);
        if (dodela.getStatus() == StatusDodeleOpreme.ZAVRSENO) {
            throw new BadRequestException("Završena dodela ne može biti otkazana.");
        }
        if (dodela.getStatus() == StatusDodeleOpreme.OTKAZANO) {
            return toDto(dodela);
        }

        Oprema oprema = dodela.getOprema();
        if (dodela.getStatus() == StatusDodeleOpreme.NA_DOGADJAJU) {
            opremaService.oslobodiSaDogadjaja(oprema, dodela.getKolicina());
        } else if (dodela.getStatus() == StatusDodeleOpreme.REZERVISANO) {
            opremaService.oslobodiRezervaciju(oprema, dodela.getKolicina());
        }

        dodela.setStatus(StatusDodeleOpreme.OTKAZANO);
        return toDto(dodelaOpremeRepository.save(dodela));
    }

    @Transactional
    public int oslobodiIstekleDodele() {
        List<DodelaOpreme> istekle = dodelaOpremeRepository.findByStatusInAndDatumDoBefore(
                List.of(StatusDodeleOpreme.REZERVISANO, StatusDodeleOpreme.NA_DOGADJAJU),
                LocalDate.now());
        int count = 0;
        for (DodelaOpreme dodela : istekle) {
            oslobodi(dodela.getDodelaId());
            count++;
        }
        return count;
    }

    private Sesija resolveSesija(Long sesijaId, Long dogadjajId) {
        if (sesijaId == null) {
            return null;
        }
        Sesija sesija = sesijaRepository.findById(sesijaId)
                .orElseThrow(() -> new NotFoundException("Sesija nije pronađena: " + sesijaId));
        if (!sesija.getDogadjaj().getDogadjajId().equals(dogadjajId)) {
            throw new BadRequestException("Sesija ne pripada izabranom događaju.");
        }
        return sesija;
    }

    private void validatePeriod(LocalDate od, LocalDate doDatuma) {
        if (od == null || doDatuma == null) {
            throw new BadRequestException("Period dodele je obavezan.");
        }
        if (doDatuma.isBefore(od)) {
            throw new BadRequestException("Datum završetka mora biti posle datuma početka.");
        }
    }

    private void zabeleziKretanje(Oprema oprema, DodelaOpreme dodela, TipInventarKretanja tip,
                                  int kolicina, String napomena) {
        Long kreiraoId = null;
        try {
            kreiraoId = currentUserService.getCurrentZaposleni().getKorisnikId();
        } catch (RuntimeException ignored) {
            // dozvoljeno za sistemsko oslobađanje
        }
        inventarKretanjeRepository.save(InventarKretanje.builder()
                .oprema(oprema)
                .tip(tip)
                .kolicina(kolicina)
                .dodela(dodela)
                .kreiraoId(kreiraoId)
                .napomena(napomena)
                .build());
    }

    private DodelaOpreme findEntity(Long id) {
        return dodelaOpremeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dodela opreme nije pronađena: " + id));
    }

    private DodelaOpremeDto toDto(DodelaOpreme entity) {
        return DodelaOpremeDto.builder()
                .dodelaId(entity.getDodelaId())
                .opremaId(entity.getOprema().getOpremaId())
                .opremaNaziv(entity.getOprema().getNaziv())
                .dogadjajId(entity.getDogadjaj().getDogadjajId())
                .dogadjajNaziv(entity.getDogadjaj().getNaziv())
                .sesijaId(entity.getSesija() != null ? entity.getSesija().getSesijaId() : null)
                .sesijaNaziv(entity.getSesija() != null ? entity.getSesija().getNaziv() : null)
                .kolicina(entity.getKolicina())
                .datumOd(entity.getDatumOd())
                .datumDo(entity.getDatumDo())
                .status(entity.getStatus())
                .napomena(entity.getNapomena())
                .kreiranAt(entity.getKreiranAt())
                .build();
    }
}
