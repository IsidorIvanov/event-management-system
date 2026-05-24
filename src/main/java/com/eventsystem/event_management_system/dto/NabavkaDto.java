package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.KriterijumSelekcijeDobavljaca;
import com.eventsystem.event_management_system.utils.enums.StatusNabavke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NabavkaDto {

    private Long nabavkaId;

    @NotNull(message = "ID događaja je obavezan")
    private Long dogadjajId;

    private String dogadjajNaziv;

    private Long kreiraoId;

    private String kreiraoIme;

    private Long dobavljacId;

    private String dobavljacNaziv;

    private StatusNabavke status;

    private BigDecimal ukupnaCena;

    private LocalDateTime kreiranAt;

    private KriterijumSelekcijeDobavljaca selekcijaKriterijum;

    @NotEmpty(message = "Nabavka mora imati bar jednu stavku")
    @Valid
    @Builder.Default
    private List<StavkaNabavkeDto> stavke = new ArrayList<>();
}
