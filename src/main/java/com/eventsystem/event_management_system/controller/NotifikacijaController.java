package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.NotifikacijaResponseDto;
import com.eventsystem.event_management_system.service.NotifikacijaService;
import com.eventsystem.event_management_system.utils.enums.TipNotifikacije;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifikacije")
@RequiredArgsConstructor
public class NotifikacijaController {

    private final NotifikacijaService notifikacijaService;

    /**
     * Stranica in-app obaveštenja trenutnog korisnika.
     *
     * @param tip  opcioni filter po tipu obaveštenja (izostavljen → svi tipovi)
     * @param page indeks strane (0-based)
     * @param size broj obaveštenja po strani
     */
    @GetMapping("/moje")
    public ResponseEntity<Page<NotifikacijaResponseDto>> getMoje(
            @RequestParam(required = false) TipNotifikacije tip,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("vremeSlanja").descending());
        return ResponseEntity.ok(notifikacijaService.getMojeNotifikacije(tip, pageable));
    }

    @GetMapping("/neprocitane/broj")
    public ResponseEntity<Long> countNeprocitane() {
        return ResponseEntity.ok(notifikacijaService.countNeprocitane());
    }

    @PostMapping("/{id}/procitano")
    public ResponseEntity<NotifikacijaResponseDto> oznaciKaoProcitano(@PathVariable Long id) {
        return ResponseEntity.ok(notifikacijaService.oznaciKaoProcitano(id));
    }
}
