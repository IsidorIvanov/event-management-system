package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.InventarPotrebeValidacijaDto;
import com.eventsystem.event_management_system.dto.DetektovanaPotrebaDto;
import com.eventsystem.event_management_system.utils.TekstNormalizacija;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarPotrebeService {

    private final PotrebaOpremeDetekcijaService potrebaOpremeDetekcijaService;
    private final OpremaService opremaService;

    @Transactional(readOnly = true)
    public InventarPotrebeValidacijaDto validirajPotrebeInventarom(Long dogadjajId) {
        List<DetektovanaPotrebaDto> potrebe = potrebaOpremeDetekcijaService.detektujPotrebe(dogadjajId);
        List<InventarPotrebeValidacijaDto.StavkaPokrivenostDto> stavke = new ArrayList<>();
        List<String> upozorenja = new ArrayList<>();
        int pokriveno = 0;

        for (DetektovanaPotrebaDto potreba : potrebe) {
            int dostupno = opremaService.dostupnaKolicinaZaNaziv(potreba.getNazivResursa());
            boolean imaDovoljno = dostupno >= potreba.getKolicina();
            if (imaDovoljno) {
                pokriveno++;
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
}
