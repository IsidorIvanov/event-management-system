package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.AnalizaProfitabilnostiDto;
import com.eventsystem.event_management_system.dto.CreateAnalizaProfitabilnostiRequest;
import com.eventsystem.event_management_system.dto.UpdateAnalizaProfitabilnostiRequest;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.AnalizaProfitabilnosti;
import com.eventsystem.event_management_system.model.Dogadjaj;
import com.eventsystem.event_management_system.model.Zaposleni;
import com.eventsystem.event_management_system.repository.AnalizaProfitabilnostiRepository;
import com.eventsystem.event_management_system.repository.DogadjajRepository;
import com.eventsystem.event_management_system.repository.FakturaRepository;
import com.eventsystem.event_management_system.repository.GovornikRepository;
import com.eventsystem.event_management_system.repository.RegistracijaRepository;
import com.eventsystem.event_management_system.repository.StavkaNabavkeRepository;
import com.eventsystem.event_management_system.repository.TrosakRepository;
import com.eventsystem.event_management_system.utils.enums.AnalizaStatus;
import com.eventsystem.event_management_system.utils.enums.RezultatOcene;
import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Agregacija profitabilnosti događaja pri finalizaciji.
 *
 * <h2>Izvori prihoda</h2>
 * <ul>
 *   <li>Registracije (B2C) — suma cena potvrđenih karata</li>
 *   <li>Izlazne fakture (B2B) — suma placeniIznos za plaćene izlazne fakture</li>
 * </ul>
 * Kanali su disjunktni: ista prodaja ne ulazi i kroz registraciju i kroz fakturu
 * (nema registracijaId na fakturi).
 *
 * <h2>Izvori troška</h2>
 * <ul>
 *   <li>Trosak NETO — realizovan rashod na budžetu (AUTO_ULAZNA, ručni/gotovinski)</li>
 *   <li>Govornik.honorar — honorari govornika (ne duplirati kao ručni Trosak)</li>
 *   <li>StavkaNabavke — commitovaniTrosak (informativno, van ukupanTrosak i rezultatOcene)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AnalizaProfitabilnostiService {

    private static final int MONEY_SCALE = 2;
    private static final int RATIO_SCALE = 4;
    private static final BigDecimal TOL_BE = new BigDecimal("0.01");
    private static final List<StatusNabavke> COMMITTED_NABAVKA_STATUSES = List.of(
            StatusNabavke.POTVRDJENA,
            StatusNabavke.U_ISPORUCI,
            StatusNabavke.ZAVRSENA
    );

    private final AnalizaProfitabilnostiRepository analizaProfitabilnostiRepository;
    private final DogadjajRepository dogadjajRepository;
    private final RegistracijaRepository registracijaRepository;
    private final FakturaRepository fakturaRepository;
    private final StavkaNabavkeRepository stavkaNabavkeRepository;
    private final GovornikRepository govornikRepository;
    private final TrosakRepository trosakRepository;
    private final CurrentUserService currentUserService;

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public AnalizaProfitabilnostiDto createDraft(
            Long dogadjajId,
            CreateAnalizaProfitabilnostiRequest request
    ) {
        if (dogadjajId == null) {
            throw new BadRequestException("ID događaja je obavezan.");
        }

        Dogadjaj dogadjaj = dogadjajRepository.findById(dogadjajId)
                .orElseThrow(() -> new NotFoundException("Događaj nije pronađen sa id: " + dogadjajId));

        if (dogadjaj.getStatus() != StatusDogadjaja.ZAVRSEN) {
            throw new BadRequestException("Analiza profitabilnosti se može kreirati samo za završen događaj.");
        }

        Zaposleni kreator = currentUserService.getCurrentZaposleni();
        BigDecimal ukupanPrihod = calculateUkupanPrihod(dogadjajId);
        BigDecimal ukupanTrosak = calculateUkupanTrosak(dogadjajId);

        AnalizaProfitabilnosti analiza = AnalizaProfitabilnosti.builder()
                .dogadjaj(dogadjaj)
                .kreirao(kreator)
                .ukupanPrihod(ukupanPrihod)
                .ukupanTrosak(ukupanTrosak)
                .datumAnalize(LocalDate.now())
                .status(AnalizaStatus.DRAFT)
                .napomene(normalizeNullable(request != null ? request.getNapomene() : null))
                .build();

        return toDto(analizaProfitabilnostiRepository.save(analiza));
    }

    @PreAuthorize("hasRole('MENADZER_DOGADJAJA')")
    @Transactional
    public AnalizaProfitabilnostiDto finalize(Long analizaId) {
        AnalizaProfitabilnosti analiza = findEntity(analizaId);
        if (analiza.getStatus() != AnalizaStatus.DRAFT) {
            throw new BadRequestException("Samo draft analiza može biti finalizovana.");
        }

        Zaposleni menadzer = currentUserService.getCurrentZaposleni();
        if (Objects.equals(analiza.getKreirao().getKorisnikId(), menadzer.getKorisnikId())) {
            throw new BadRequestException("Kreator analize ne može finalizovati istu analizu.");
        }

        Long dogadjajId = analiza.getDogadjaj().getDogadjajId();
        analiza.setUkupanPrihod(calculateUkupanPrihod(dogadjajId));
        analiza.setUkupanTrosak(calculateUkupanTrosak(dogadjajId));
        analiza.setRezultatOcene(classifyResult(analiza.getUkupanPrihod(), analiza.getUkupanTrosak()));
        analiza.setFinalizovanoAt(LocalDateTime.now());
        analiza.setStatus(AnalizaStatus.FINALIZOVANA);

        return toDto(analizaProfitabilnostiRepository.save(analiza));
    }

    @PreAuthorize("hasRole('FINANSIJSKI_KONTROLOR')")
    @Transactional
    public AnalizaProfitabilnostiDto updateDraft(
            Long analizaId,
            UpdateAnalizaProfitabilnostiRequest request
    ) {
        AnalizaProfitabilnosti analiza = findEntity(analizaId);
        if (analiza.getStatus() != AnalizaStatus.DRAFT) {
            throw new BadRequestException("Finalizovana analiza se ne može menjati.");
        }

        analiza.setNapomene(normalizeNullable(request != null ? request.getNapomene() : null));
        return toDto(analizaProfitabilnostiRepository.save(analiza));
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public AnalizaProfitabilnostiDto getById(Long analizaId) {
        return toDto(findEntity(analizaId));
    }

    @PreAuthorize("hasAnyRole('FINANSIJSKI_KONTROLOR', 'MENADZER_DOGADJAJA')")
    @Transactional(readOnly = true)
    public List<AnalizaProfitabilnostiDto> getByDogadjaj(Long dogadjajId) {
        if (dogadjajId == null) {
            throw new BadRequestException("ID događaja je obavezan.");
        }
        return analizaProfitabilnostiRepository.findByDogadjajDogadjajIdOrderByDatumAnalizeDesc(dogadjajId).stream()
                .map(this::toDto)
                .toList();
    }

    /** Vidi class-level JavaDoc — Izvori prihoda. */
    private BigDecimal calculateUkupanPrihod(Long dogadjajId) {
        return money(registracijaRepository.sumPrihodOdRegistracija(dogadjajId))
                .add(money(fakturaRepository.sumNetoIzlazniPrihodByDogadjaj(dogadjajId)))
                .add(sumPrihodSponzorstvaPlaceholder(dogadjajId))
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    /**
     * Realizovan trošak: Trosak NETO + honorari govornika.
     * Honorar se evidentira isključivo kroz {@code Govornik.honorar}; operativno se ne knjiži
     * ponovo kao ručni Trosak. Duplo knjiženje nije tehnički blokirano u P1.
     */
    private BigDecimal calculateUkupanTrosak(Long dogadjajId) {
        return money(trosakRepository.sumNetoByDogadjaj(dogadjajId))
                .add(money(govornikRepository.sumHonorarByDogadjaj(dogadjajId)))
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calculateCommitovaniTrosak(Long dogadjajId) {
        return money(stavkaNabavkeRepository.sumTrosakStavkiNabavke(dogadjajId, COMMITTED_NABAVKA_STATUSES))
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private BigDecimal sumPrihodSponzorstvaPlaceholder(Long dogadjajId) {
        // Placeholder — entitet ne postoji u trenutnom modelu, vraća 0
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    RezultatOcene classifyResult(BigDecimal prihod, BigDecimal trosak) {
        BigDecimal safePrihod = money(prihod);
        BigDecimal safeTrosak = money(trosak);
        if (safePrihod.compareTo(BigDecimal.ZERO) == 0) {
            return safeTrosak.compareTo(BigDecimal.ZERO) == 0
                    ? RezultatOcene.BREAK_EVEN
                    : RezultatOcene.GUBITAK;
        }

        BigDecimal neto = safePrihod.subtract(safeTrosak);
        BigDecimal tolerancija = safePrihod.multiply(TOL_BE);

        if (neto.compareTo(tolerancija) > 0) {
            return RezultatOcene.PROFITABILAN;
        }
        if (neto.abs().compareTo(tolerancija) <= 0) {
            return RezultatOcene.BREAK_EVEN;
        }
        return RezultatOcene.GUBITAK;
    }

    private AnalizaProfitabilnosti findEntity(Long analizaId) {
        if (analizaId == null) {
            throw new BadRequestException("ID analize je obavezan.");
        }
        return analizaProfitabilnostiRepository.findByIdWithDetalji(analizaId)
                .orElseThrow(() -> new NotFoundException("Analiza profitabilnosti nije pronađena sa id: " + analizaId));
    }

    private AnalizaProfitabilnostiDto toDto(AnalizaProfitabilnosti analiza) {
        Long dogadjajId = analiza.getDogadjaj().getDogadjajId();
        boolean isDraft = analiza.getStatus() == AnalizaStatus.DRAFT;

        // Draft: trošak live (bez nabavke u oceni). Prihod: snapshot pri kreiranju drafta.
        // Breakdown polja i dalje prikazuju trenutno stanje izvora u bazi.
        BigDecimal prihod = money(analiza.getUkupanPrihod());
        BigDecimal trosak = isDraft
                ? calculateUkupanTrosak(dogadjajId)
                : money(analiza.getUkupanTrosak());
        BigDecimal neto = prihod.subtract(trosak).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);

        BigDecimal commitovaniTrosak = isDraft
                ? calculateCommitovaniTrosak(dogadjajId)
                : null;

        return AnalizaProfitabilnostiDto.builder()
                .analizaId(analiza.getAnalizaId())
                .dogadjajId(dogadjajId)
                .dogadjajNaziv(analiza.getDogadjaj().getNaziv())
                .kreiraoId(analiza.getKreirao().getKorisnikId())
                .kreiraoImePrezime(formatImePrezime(analiza.getKreirao()))
                .ukupanPrihod(prihod)
                .ukupanTrosak(trosak)
                .commitovaniTrosak(commitovaniTrosak)
                .prihodRegistracije(isDraft
                        ? money(registracijaRepository.sumPrihodOdRegistracija(dogadjajId))
                        : null)
                .prihodIzlazneFakture(isDraft
                        ? money(fakturaRepository.sumNetoIzlazniPrihodByDogadjaj(dogadjajId))
                        : null)
                .trosakEvidentiran(isDraft
                        ? money(trosakRepository.sumNetoByDogadjaj(dogadjajId))
                        : null)
                .trosakHonorari(isDraft
                        ? money(govornikRepository.sumHonorarByDogadjaj(dogadjajId))
                        : null)
                .neto(neto)
                .marza(prihod.compareTo(BigDecimal.ZERO) > 0
                        ? neto.divide(prihod, RATIO_SCALE, RoundingMode.HALF_EVEN)
                        : null)
                .roi(trosak.compareTo(BigDecimal.ZERO) > 0
                        ? neto.divide(trosak, RATIO_SCALE, RoundingMode.HALF_EVEN)
                        : null)
                .datumAnalize(analiza.getDatumAnalize())
                .finalizovanoAt(analiza.getFinalizovanoAt())
                .rezultatOcene(analiza.getRezultatOcene())
                .status(analiza.getStatus())
                .napomene(analiza.getNapomene())
                .build();
    }

    private BigDecimal money(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private String formatImePrezime(Zaposleni zaposleni) {
        return (zaposleni.getIme() + " " + zaposleni.getPrezime()).trim();
    }

    private String normalizeNullable(String value) {
        return value != null && !value.trim().isEmpty() ? value.trim() : null;
    }
}
