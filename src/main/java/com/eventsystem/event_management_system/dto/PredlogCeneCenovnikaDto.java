package com.eventsystem.event_management_system.dto;



import lombok.*;



import java.math.BigDecimal;



@Builder

@Getter

@Setter

@NoArgsConstructor

@AllArgsConstructor

public class PredlogCeneCenovnikaDto {



    private Long cenovnikId;

    private String nazivResursa;

    private Long dobavljacId;

    private String dobavljacNaziv;

    private BigDecimal trenutnaCena;

    private BigDecimal predlozenaCena;

    private int ukupnaPotraznjaKolicina;

    private int brojDogadjajaSaPotrebom;

    private int potraznjaIndeksProcenat;

    private String pravilo;

    private BigDecimal promenaProcenat;

}

