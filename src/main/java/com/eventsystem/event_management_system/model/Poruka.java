package com.eventsystem.event_management_system.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "poruka")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Poruka {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "poruka_id")
    private Long porukaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posiljalac_id", nullable = false)
    private Korisnik posiljalac;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primalac_id", nullable = false)
    private Korisnik primalac;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sadrzaj;

    @Column(name = "vreme_slanja")
    private LocalDateTime vremeSlamja;

    @Column(name = "procitano")
    private boolean procitano = false;

    @PrePersist
    protected void onCreate() {
        if (vremeSlamja == null) {
            vremeSlamja = LocalDateTime.now();
        }
    }
}

