package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.OpremaDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.Cenovnik;
import com.eventsystem.event_management_system.model.Oprema;
import com.eventsystem.event_management_system.repository.CenovnikRepository;
import com.eventsystem.event_management_system.repository.OpremaRepository;
import com.eventsystem.event_management_system.utils.TekstNormalizacija;
import com.eventsystem.event_management_system.utils.enums.StatusOpreme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OpremaService {

    private final OpremaRepository opremaRepository;
    private final CenovnikRepository cenovnikRepository;

    @Transactional(readOnly = true)
    public List<OpremaDto> getAll() {
        return opremaRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OpremaDto getById(Long id) {
        return toDto(findEntity(id));
    }

    @Transactional
    public OpremaDto create(OpremaDto dto) {
        int ukupno = dto.getKolicinaUkupno() != null ? dto.getKolicinaUkupno() : 0;
        int dostupno = dto.getKolicinaDostupno() != null ? dto.getKolicinaDostupno() : ukupno;
        validateKolicine(ukupno, dostupno, 0, 0, 0);

        Oprema entity = Oprema.builder()
                .naziv(normalizeRequired(dto.getNaziv(), "Naziv"))
                .kategorija(normalizeRequired(dto.getKategorija(), "Kategorija"))
                .cenovnik(resolveCenovnik(dto.getCenovnikId()))
                .kolicinaUkupno(ukupno)
                .kolicinaDostupno(dostupno)
                .kolicinaRezervisano(0)
                .kolicinaNaDogadjaju(0)
                .kolicinaUServisu(0)
                .status(dto.getStatus() != null ? dto.getStatus() : StatusOpreme.DOSTUPNO)
                .lokacijaSkladista(dto.getLokacijaSkladista())
                .serijskiBroj(dto.getSerijskiBroj())
                .napomena(dto.getNapomena())
                .build();
        return toDto(opremaRepository.save(entity));
    }

    @Transactional
    public OpremaDto update(Long id, OpremaDto dto) {
        Oprema entity = findEntity(id);
        entity.setNaziv(normalizeRequired(dto.getNaziv(), "Naziv"));
        entity.setKategorija(normalizeRequired(dto.getKategorija(), "Kategorija"));
        entity.setCenovnik(resolveCenovnik(dto.getCenovnikId()));
        entity.setLokacijaSkladista(dto.getLokacijaSkladista());
        entity.setSerijskiBroj(dto.getSerijskiBroj());
        entity.setNapomena(dto.getNapomena());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }

        int rezervisano = entity.getKolicinaRezervisano();
        int naDogadjaju = entity.getKolicinaNaDogadjaju();
        int uServisu = entity.getKolicinaUServisu();
        int ukupno = dto.getKolicinaUkupno() != null ? dto.getKolicinaUkupno() : entity.getKolicinaUkupno();
        int dostupno = dto.getKolicinaDostupno() != null ? dto.getKolicinaDostupno() : entity.getKolicinaDostupno();
        validateKolicine(ukupno, dostupno, rezervisano, naDogadjaju, uServisu);
        entity.setKolicinaUkupno(ukupno);
        entity.setKolicinaDostupno(dostupno);
        osveziStatus(entity);
        return toDto(opremaRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        Oprema entity = findEntity(id);
        if (entity.getKolicinaRezervisano() > 0 || entity.getKolicinaNaDogadjaju() > 0) {
            throw new BadRequestException("Oprema sa aktivnim rezervacijama ne može biti obrisana.");
        }
        opremaRepository.delete(entity);
    }

    @Transactional
    public Oprema findOrCreateByNaziv(String nazivResursa, String kategorija) {
        return opremaRepository.findAll().stream()
                .filter(o -> TekstNormalizacija.naziviSePodudaraju(o.getNaziv(), nazivResursa))
                .findFirst()
                .orElseGet(() -> opremaRepository.save(Oprema.builder()
                        .naziv(nazivResursa.trim())
                        .kategorija(kategorija != null ? kategorija : odrediKategoriju(nazivResursa))
                        .kolicinaUkupno(0)
                        .kolicinaDostupno(0)
                        .kolicinaRezervisano(0)
                        .kolicinaNaDogadjaju(0)
                        .kolicinaUServisu(0)
                        .status(StatusOpreme.DOSTUPNO)
                        .build()));
    }

    @Transactional
    public void uvecanjeZalihe(Oprema oprema, int kolicina) {
        if (kolicina <= 0) {
            throw new BadRequestException("Količina mora biti pozitivna.");
        }
        oprema.setKolicinaUkupno(oprema.getKolicinaUkupno() + kolicina);
        oprema.setKolicinaDostupno(oprema.getKolicinaDostupno() + kolicina);
        osveziStatus(oprema);
        opremaRepository.save(oprema);
    }

    @Transactional
    public void rezervisi(Oprema oprema, int kolicina) {
        if (kolicina <= 0) {
            throw new BadRequestException("Količina mora biti pozitivna.");
        }
        if (oprema.getKolicinaDostupno() < kolicina) {
            throw new BadRequestException("Nedovoljna količina na stanju za: " + oprema.getNaziv());
        }
        oprema.setKolicinaDostupno(oprema.getKolicinaDostupno() - kolicina);
        oprema.setKolicinaRezervisano(oprema.getKolicinaRezervisano() + kolicina);
        osveziStatus(oprema);
        opremaRepository.save(oprema);
    }

    @Transactional
    public void aktivirajRezervaciju(Oprema oprema, int kolicina) {
        if (oprema.getKolicinaRezervisano() < kolicina) {
            throw new BadRequestException("Nedovoljno rezervisane količine.");
        }
        oprema.setKolicinaRezervisano(oprema.getKolicinaRezervisano() - kolicina);
        oprema.setKolicinaNaDogadjaju(oprema.getKolicinaNaDogadjaju() + kolicina);
        osveziStatus(oprema);
        opremaRepository.save(oprema);
    }

    @Transactional
    public void oslobodiSaDogadjaja(Oprema oprema, int kolicina) {
        if (oprema.getKolicinaNaDogadjaju() < kolicina) {
            throw new BadRequestException("Količina na događaju nije ispravna.");
        }
        oprema.setKolicinaNaDogadjaju(oprema.getKolicinaNaDogadjaju() - kolicina);
        oprema.setKolicinaDostupno(oprema.getKolicinaDostupno() + kolicina);
        osveziStatus(oprema);
        opremaRepository.save(oprema);
    }

    @Transactional
    public void oslobodiRezervaciju(Oprema oprema, int kolicina) {
        if (oprema.getKolicinaRezervisano() < kolicina) {
            throw new BadRequestException("Količina rezervacije nije ispravna.");
        }
        oprema.setKolicinaRezervisano(oprema.getKolicinaRezervisano() - kolicina);
        oprema.setKolicinaDostupno(oprema.getKolicinaDostupno() + kolicina);
        osveziStatus(oprema);
        opremaRepository.save(oprema);
    }

    public Oprema findEntity(Long id) {
        return opremaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Oprema nije pronađena sa id: " + id));
    }

    public int dostupnaKolicinaZaNaziv(String nazivResursa) {
        return opremaRepository.findAll().stream()
                .filter(o -> TekstNormalizacija.naziviSePodudaraju(o.getNaziv(), nazivResursa))
                .mapToInt(Oprema::getKolicinaDostupno)
                .sum();
    }

    void osveziStatus(Oprema oprema) {
        if (oprema.getStatus() == StatusOpreme.OTPISANO || oprema.getStatus() == StatusOpreme.U_SERVISU) {
            return;
        }
        if (oprema.getKolicinaNaDogadjaju() > 0) {
            oprema.setStatus(StatusOpreme.NA_DOGADJAJU);
        } else if (oprema.getKolicinaRezervisano() > 0) {
            oprema.setStatus(StatusOpreme.REZERVISANO);
        } else if (oprema.getKolicinaDostupno() > 0) {
            oprema.setStatus(StatusOpreme.DOSTUPNO);
        } else if (oprema.getKolicinaUkupno() == 0) {
            oprema.setStatus(StatusOpreme.DOSTUPNO);
        }
    }

    private Cenovnik resolveCenovnik(Long cenovnikId) {
        if (cenovnikId == null) {
            return null;
        }
        return cenovnikRepository.findById(cenovnikId)
                .orElseThrow(() -> new NotFoundException("Stavka cenovnika nije pronađena: " + cenovnikId));
    }

    private String odrediKategoriju(String naziv) {
        String n = TekstNormalizacija.normalizuj(naziv);
        if (n.contains("zvucnik") || n.contains("mikrofon") || n.contains("audio")) {
            return "AUDIO";
        }
        if (n.contains("led") || n.contains("rasvet")) {
            return "RASVETA";
        }
        if (n.contains("bina") || n.contains("scena")) {
            return "SCENA";
        }
        return "OSTALO";
    }

    private void validateKolicine(int ukupno, int dostupno, int rezervisano, int naDogadjaju, int uServisu) {
        if (ukupno < 0 || dostupno < 0) {
            throw new BadRequestException("Količine ne mogu biti negativne.");
        }
        if (dostupno + rezervisano + naDogadjaju + uServisu > ukupno) {
            throw new BadRequestException("Zbir raspodeljenih količina ne može biti veći od ukupne.");
        }
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(field + " je obavezan.");
        }
        return value.trim();
    }

    private OpremaDto toDto(Oprema entity) {
        return OpremaDto.builder()
                .opremaId(entity.getOpremaId())
                .naziv(entity.getNaziv())
                .kategorija(entity.getKategorija())
                .cenovnikId(entity.getCenovnik() != null ? entity.getCenovnik().getCenovnikId() : null)
                .kolicinaUkupno(entity.getKolicinaUkupno())
                .kolicinaDostupno(entity.getKolicinaDostupno())
                .kolicinaRezervisano(entity.getKolicinaRezervisano())
                .kolicinaNaDogadjaju(entity.getKolicinaNaDogadjaju())
                .kolicinaUServisu(entity.getKolicinaUServisu())
                .status(entity.getStatus())
                .lokacijaSkladista(entity.getLokacijaSkladista())
                .serijskiBroj(entity.getSerijskiBroj())
                .napomena(entity.getNapomena())
                .build();
    }
}
