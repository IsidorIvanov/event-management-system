package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.StatusDobavljaca;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@Getter
@Setter
@Table(name = "dobavljac")
@NoArgsConstructor
@AllArgsConstructor
public class Dobavljac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dobavljac_id")
    private Long dobavljacId;

    @Column(nullable = false, length = 200)
    private String naziv;

    @Column(nullable = false, length = 100)
    private String grad;

    @Column(name = "kontakt_email", nullable = false, length = 150)
    private String kontaktEmail;

    @Column(length = 30)
    private String telefon;

    @Column(nullable = false, length = 20)
    private String pib;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDobavljaca status;

    @Column(precision = 3, scale = 2)
    private BigDecimal rejting;

    @Column(columnDefinition = "TEXT")
    private String kriterijumi;

    @OneToMany(mappedBy = "dobavljac", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Cenovnik> cenovnici = new HashSet<>();

    @OneToMany(mappedBy = "dobavljac")
    @Builder.Default
    private Set<Ugovor> ugovori = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = StatusDobavljaca.AKTIVAN;
        }
    }
}
