package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.InventarKretanjeDto;
import com.eventsystem.event_management_system.exception.BadRequestException;
import com.eventsystem.event_management_system.exception.NotFoundException;
import com.eventsystem.event_management_system.model.InventarKretanje;
import com.eventsystem.event_management_system.model.Oprema;
import com.eventsystem.event_management_system.model.Porudzbenica;
import com.eventsystem.event_management_system.model.StavkaPorudzbenice;
import com.eventsystem.event_management_system.repository.InventarKretanjeRepository;
import com.eventsystem.event_management_system.utils.enums.StatusPorudzbenice;
import com.eventsystem.event_management_system.utils.enums.TipInventarKretanja;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarService {

    private final InventarKretanjeRepository inventarKretanjeRepository;
    private final PorudzbenicaService porudzbenicaService;
    private final OpremaService opremaService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<InventarKretanjeDto> getKretanjaZaOprema(Long opremaId) {
        return inventarKretanjeRepository.findByOpremaOpremaIdOrderByKreiranAtDesc(opremaId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean jePrimljenaPorudzbenica(Long porudzbenicaId) {
        return inventarKretanjeRepository.existsByPorudzbenicaPorudzbenicaIdAndTip(
                porudzbenicaId, TipInventarKretanja.ULAZ);
    }

    @Transactional
    public List<InventarKretanjeDto> primiIzPorudzbenice(Long porudzbenicaId) {
        Porudzbenica porudzbenica = porudzbenicaService.findEntityWithDetalji(porudzbenicaId);

        if (porudzbenica.getStatus() != StatusPorudzbenice.ISPORUCENA) {
            throw new BadRequestException("Prijem u inventar je moguć samo za isporučene porudžbenice.");
        }
        if (inventarKretanjeRepository.existsByPorudzbenicaPorudzbenicaIdAndTip(
                porudzbenicaId, TipInventarKretanja.ULAZ)) {
            throw new BadRequestException("Ova porudžbenica je već primljena u inventar.");
        }
        if (porudzbenica.getStavke() == null || porudzbenica.getStavke().isEmpty()) {
            throw new BadRequestException("Porudžbenica nema stavki za prijem.");
        }

        Long kreiraoId = currentUserService.getCurrentZaposleni().getKorisnikId();
        List<InventarKretanjeDto> kretanja = new ArrayList<>();

        for (StavkaPorudzbenice stavka : porudzbenica.getStavke()) {
            Oprema oprema = opremaService.findOrCreateByNaziv(stavka.getNazivResursa(), null);
            opremaService.uvecanjeZalihe(oprema, stavka.getKolicina());

            InventarKretanje kretanje = InventarKretanje.builder()
                    .oprema(oprema)
                    .tip(TipInventarKretanja.ULAZ)
                    .kolicina(stavka.getKolicina())
                    .porudzbenica(porudzbenica)
                    .kreiraoId(kreiraoId)
                    .napomena("Prijem iz porudžbenice " + porudzbenica.getBrojPorudzbenice())
                    .build();
            kretanja.add(toDto(inventarKretanjeRepository.save(kretanje)));
        }

        return kretanja;
    }

    private InventarKretanjeDto toDto(InventarKretanje entity) {
        return InventarKretanjeDto.builder()
                .kretanjeId(entity.getKretanjeId())
                .opremaId(entity.getOprema().getOpremaId())
                .opremaNaziv(entity.getOprema().getNaziv())
                .tip(entity.getTip())
                .kolicina(entity.getKolicina())
                .porudzbenicaId(entity.getPorudzbenica() != null ? entity.getPorudzbenica().getPorudzbenicaId() : null)
                .dodelaId(entity.getDodela() != null ? entity.getDodela().getDodelaId() : null)
                .kreiraoId(entity.getKreiraoId())
                .kreiranAt(entity.getKreiranAt())
                .napomena(entity.getNapomena())
                .build();
    }
}
