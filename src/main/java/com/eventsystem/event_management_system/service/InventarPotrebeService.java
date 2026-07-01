package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.DetektovanaPotrebaDto;
import com.eventsystem.event_management_system.dto.InventarPotrebeValidacijaDto;
import com.eventsystem.event_management_system.model.DodelaOpreme;
import com.eventsystem.event_management_system.repository.DodelaOpremeRepository;
import com.eventsystem.event_management_system.utils.TekstNormalizacija;
import com.eventsystem.event_management_system.utils.enums.StatusDodeleOpreme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarPotrebeService {

    private static final List<StatusDodeleOpreme> AKTIVNE_DODELE = List.of(
            StatusDodeleOpreme.REZERVISANO,
            StatusDodeleOpreme.NA_DOGADJAJU
    );

    private final PotrebaOpremeDetekcijaService potrebaOpremeDetekcijaService;
    private final OpremaService opremaService;
    private final DodelaOpremeRepository dodelaOpremeRepository;

    @Transactional(readOnly = true)
    public InventarPotrebeValidacijaDto validirajPotrebeInventarom(Long dogadjajId) {
        List<DetektovanaPotrebaDto> potrebe = potrebaOpremeDetekcijaService.detektujPotrebe(dogadjajId);
        List<DodelaOpreme> aktivneDodele = dodelaOpremeRepository.findByDogadjajWithDetalji(dogadjajId).stream()
                .filter(d -> d.getStatus() != null && AKTIVNE_DODELE.contains(d.getStatus()))
                .toList();

        List<InventarPotrebeValidacijaDto.StavkaPokrivenostDto> stavke = new ArrayList<>();
        List<String> upozorenja = new ArrayList<>();
        int pokriveno = 0;

        for (DetektovanaPotrebaDto potreba : potrebe) {
            int dodeljeno = sumDodeljeno(aktivneDodele, potreba.getNazivResursa());
            int dostupno = opremaService.dostupnaKolicinaZaNaziv(potreba.getNazivResursa());
            boolean imaDovoljno = dodeljeno >= potreba.getKolicina();
            if (imaDovoljno) {
                pokriveno++;
            } else if (dodeljeno > 0) {
                upozorenja.add(String.format(
                        "Delimično dodeljeno: %s (dodeljeno %d, potrebno %d)",
                        potreba.getNazivResursa(), dodeljeno, potreba.getKolicina()));
            } else {
                upozorenja.add(String.format(
                        "Nedovoljno na stanju: %s (potrebno %d, dostupno %d)",
                        potreba.getNazivResursa(), potreba.getKolicina(), dostupno));
            }
            stavke.add(InventarPotrebeValidacijaDto.StavkaPokrivenostDto.builder()
                    .nazivResursa(potreba.getNazivResursa())
                    .potrebnaKolicina(potreba.getKolicina())
                    .dostupnoNaStanju(dostupno)
                    .pokriveno(imaDovoljno)
                    .build());
        }

        if (potrebe.isEmpty()) {
            upozorenja.add("Nema detektovanih potreba ili cenovnik/inventar nije popunjen.");
        }

        int ukupno = potrebe.size();
        int procenat = ukupno == 0 ? 0 : (pokriveno * 100) / ukupno;

        return InventarPotrebeValidacijaDto.builder()
                .dogadjajId(dogadjajId)
                .svePokriveno(ukupno > 0 && pokriveno == ukupno)
                .tacnostProcenat(procenat)
                .stavke(stavke)
                .upozorenja(upozorenja)
                .build();
    }

    private int sumDodeljeno(List<DodelaOpreme> dodele, String nazivPotrebe) {
        return dodele.stream()
                .filter(d -> TekstNormalizacija.naziviSePodudaraju(d.getOprema().getNaziv(), nazivPotrebe))
                .mapToInt(DodelaOpreme::getKolicina)
                .sum();
    }
}
