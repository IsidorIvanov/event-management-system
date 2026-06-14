package com.eventsystem.event_management_system.controller;

import com.eventsystem.event_management_system.dto.DogadjajKontaktiDto;
import com.eventsystem.event_management_system.dto.PreporukaDto;
import com.eventsystem.event_management_system.service.PreporukaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/preporuka")
@RequiredArgsConstructor
public class PreporukaController {

    private final PreporukaService preporukaService;

    /**
     * Generiše (ponovo izračunava) i vraća preporuke za trenutnog učesnika.
     */
    @GetMapping("/moje")
    public ResponseEntity<List<PreporukaDto>> getMojePreporuke() {
        return ResponseEntity.ok(preporukaService.generisiMojePreporuke());
    }

    /**
     * Kontakti (organizatori i učesnici) za preporučeni događaj.
     */
    @GetMapping("/{dogadjajId}/kontakti")
    public ResponseEntity<DogadjajKontaktiDto> getKontakti(@PathVariable Long dogadjajId) {
        return ResponseEntity.ok(preporukaService.getKontaktiZaDogadjaj(dogadjajId));
    }
}
