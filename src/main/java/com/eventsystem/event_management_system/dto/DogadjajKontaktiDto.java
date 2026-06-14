package com.eventsystem.event_management_system.dto;

import lombok.*;

import java.util.List;

/** Kontakti vezani za jedan događaj — organizatori i registrovani učesnici. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DogadjajKontaktiDto {
    private List<KontaktKorisnikaDto> organizatori;
    private List<KontaktKorisnikaDto> ucesnici;
}
