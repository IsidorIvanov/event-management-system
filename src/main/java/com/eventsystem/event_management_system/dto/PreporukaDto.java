package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.StatusDogadjaja;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PreporukaDto {

    private Long preporukaId;
    private BigDecimal skorPoklapanja;
    private String razlog;
    private LocalDate datumGenerisanja;

    /** Interesovanja učesnika koja se poklapaju sa tagovima događaja. */
    private List<String> zajednickiTagovi;

    private Long dogadjajId;
    private String naziv;
    private LocalDate datumPocetka;
    private LocalDate datumZavrsetka;
    private String opis;
    private StatusDogadjaja status;
    private Set<String> tagovi;

    private String lokacijaNaziv;
    private String lokacijaGrad;
    private String lokacijaDrzava;
}
