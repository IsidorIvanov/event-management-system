package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.NabavkaDto;
import com.eventsystem.event_management_system.dto.StavkaNabavkeDto;
import com.eventsystem.event_management_system.model.*;
import com.eventsystem.event_management_system.model.compositePK.StavkaNabavkeId;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.NabavkaRepository;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class NabavkaService {

    private final NabavkaRepository nabavkaRepository;
    private final DogadjajRepository dogadjajRepository;
    private final DobavljacService dobavljacService;
    private final CenovnikService cenovnikService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<NabavkaDto> getAll() {
        return nabavkaRepository.findAllWithDetalji().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<NabavkaDto> getByDogadjaj(Long dogadjajId) {
        return nabavkaRepository.findByDogadjajDogadjajIdOrderByKreiranAtDesc(dogadjajId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public NabavkaDto getById(Long id) {
        Nabavka nabavka = nabavkaRepository.findByIdWithDetalji(id)
                .orElseThrow(() -> new RuntimeException("Nabavka nije pronadjena sa id: " + id));
        return toDto(nabavka);
    }

    @Transactional
    public NabavkaDto create(NabavkaDto dto) {
        Dogadjaj dogadjaj = dogadjajRepository.findById(dto.getDogadjajId())
                .orElseThrow(() -> new RuntimeException("Dogadjaj nije pronadjen sa id: " + dto.getDogadjajId()));
        Zaposleni kreirao = currentUserService.getCurrentZaposleni();

        Nabavka nabavka = Nabavka.builder()
                .dogadjaj(dogadjaj)
                .kreirao(kreirao)
                .status(StatusNabavke.NACRT)
                .selekcijaKriterijum(dto.getSelekcijaKriterijum())
                .stavke(new ArrayList<>())
                .build();

        Nabavka saved = nabavkaRepository.save(nabavka);
        applyStavke(saved, dto.getStavke());
        recalculateTotal(saved);
        dodeliDobavljacaIzStavki(saved);
        saved = nabavkaRepository.save(saved);
        return getById(saved.getNabavkaId());
    }

    @Transactional
    public NabavkaDto updateStatus(Long id, StatusNabavke status) {
        Nabavka nabavka = findEntity(id);
        nabavka.setStatus(status);
        return toDto(nabavkaRepository.saveAndFlush(nabavka));
    }

    public NabavkaDto assignDobavljac(Long nabavkaId, Long dobavljacId) {
        Nabavka nabavka = findEntity(nabavkaId);
        dobavljacService.assertAvailableForNabavka(dobavljacId);
        Dobavljac dobavljac = dobavljacService.findEntity(dobavljacId);
        nabavka.setDobavljac(dobavljac);
        nabavka.setStatus(StatusNabavke.PREDLOZENA);
        return toDto(nabavkaRepository.save(nabavka));
    }

    public Nabavka findEntity(Long id) {
        return nabavkaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nabavka nije pronadjena sa id: " + id));
    }

    public Nabavka findEntityWithDetalji(Long id) {
        return nabavkaRepository.findByIdWithDetalji(id)
                .orElseThrow(() -> new RuntimeException("Nabavka nije pronadjena sa id: " + id));
    }

    @Transactional
    public void applyPredlogStavki(Nabavka nabavka, List<StavkaNabavkeDto> stavkeDto) {
        nabavka.getStavke().clear();
        applyStavke(nabavka, stavkeDto);
        recalculateTotal(nabavka);
        nabavkaRepository.saveAndFlush(nabavka);
    }

    private void applyStavke(Nabavka nabavka, List<StavkaNabavkeDto> stavkeDto) {
        AtomicInteger redni = new AtomicInteger(1);
        for (StavkaNabavkeDto s : stavkeDto) {
            BigDecimal jedinicna = s.getJedinicnaCena() != null ? s.getJedinicnaCena() : BigDecimal.ZERO;
            int rb = redni.getAndIncrement();
            StavkaNabavkeId id = new StavkaNabavkeId(nabavka.getNabavkaId(), rb);

            var builder = StavkaNabavke.builder()
                    .id(id)
                    .nabavka(nabavka)
                    .nazivResursa(s.getNazivResursa())
                    .kolicina(s.getKolicina())
                    .jedinicnaCena(jedinicna)
                    .opis(s.getOpis());

            if (s.getCenovnikId() != null) {
                Cenovnik cenovnik = cenovnikService.findEntity(s.getCenovnikId());
                builder.cenovnik(cenovnik);
                if (s.getJedinicnaCena() == null) {
                    builder.jedinicnaCena(cenovnik.getCenaJedinicna());
                }
            }

            StavkaNabavke stavka = builder.build();
            stavka.osveziUkupnuCenu();
            nabavka.getStavke().add(stavka);
        }
    }

    private void recalculateTotal(Nabavka nabavka) {
        BigDecimal total = nabavka.getStavke().stream()
                .map(StavkaNabavke::getUkupnaCena)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        nabavka.setUkupnaCena(total);
    }

    /**
     * Pri ručnoj nabavci iz cenovnika: dodeljuje dobavljača sa najvećim udelom u vrednosti stavki.
     */
    private void dodeliDobavljacaIzStavki(Nabavka nabavka) {
        Map<Long, BigDecimal> vrednostPoDobavljacu = new HashMap<>();

        for (StavkaNabavke stavka : nabavka.getStavke()) {
            if (stavka.getCenovnik() == null || stavka.getCenovnik().getDobavljac() == null) {
                continue;
            }
            Long dobavljacId = stavka.getCenovnik().getDobavljac().getDobavljacId();
            vrednostPoDobavljacu.merge(dobavljacId, stavka.getUkupnaCena(), BigDecimal::add);
        }

        if (vrednostPoDobavljacu.isEmpty()) {
            return;
        }

        Long izabraniId = vrednostPoDobavljacu.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow();

        dobavljacService.assertAvailableForNabavka(izabraniId);
        nabavka.setDobavljac(dobavljacService.findEntity(izabraniId));
        if (nabavka.getStatus() == StatusNabavke.NACRT) {
            nabavka.setStatus(StatusNabavke.PREDLOZENA);
        }
    }

    private NabavkaDto toDto(Nabavka entity) {
        return NabavkaDto.builder()
                .nabavkaId(entity.getNabavkaId())
                .dogadjajId(entity.getDogadjaj().getDogadjajId())
                .dogadjajNaziv(entity.getDogadjaj().getNaziv())
                .kreiraoId(entity.getKreirao().getKorisnikId())
                .kreiraoIme(entity.getKreirao().getIme() + " " + entity.getKreirao().getPrezime())
                .dobavljacId(entity.getDobavljac() != null ? entity.getDobavljac().getDobavljacId() : null)
                .dobavljacNaziv(entity.getDobavljac() != null ? entity.getDobavljac().getNaziv() : null)
                .status(entity.getStatus())
                .ukupnaCena(entity.getUkupnaCena())
                .kreiranAt(entity.getKreiranAt())
                .selekcijaKriterijum(entity.getSelekcijaKriterijum())
                .stavke(entity.getStavke().stream().map(this::toStavkaDto).toList())
                .build();
    }

    private StavkaNabavkeDto toStavkaDto(StavkaNabavke s) {
        return StavkaNabavkeDto.builder()
                .redniBroj(s.getId().getRedniBroj())
                .nazivResursa(s.getNazivResursa())
                .kolicina(s.getKolicina())
                .jedinicnaCena(s.getJedinicnaCena())
                .ukupnaCena(s.getUkupnaCena())
                .opis(s.getOpis())
                .cenovnikId(s.getCenovnik() != null ? s.getCenovnik().getCenovnikId() : null)
                .build();
    }
}
