package com.eventsystem.event_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudzetKategorijaDto {

    private Long kategorijaId;

    @NotBlank(message = "Naziv kategorije je obavezan")
    @Size(max = 100, message = "Naziv kategorije može imati najviše 100 karaktera")
    private String naziv;

    @Size(max = 500, message = "Opis može imati najviše 500 karaktera")
    private String opis;
}
