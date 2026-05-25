package com.eventsystem.event_management_system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@Getter
@Setter
@Table(name = "budzet_kategorija")
@NoArgsConstructor
@AllArgsConstructor
public class BudzetKategorija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kategorija_id")
    private Long kategorijaId;

    @Column(nullable = false, unique = true, length = 100)
    private String naziv;

    @Column(length = 500)
    private String opis;
}
