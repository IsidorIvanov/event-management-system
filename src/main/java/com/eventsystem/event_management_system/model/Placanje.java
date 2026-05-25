package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.MetodPlacanja;
import com.eventsystem.event_management_system.utils.enums.PlacanjeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@Table(name = "placanje")
@NoArgsConstructor
@AllArgsConstructor
public class Placanje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "placanje_id")
    private Long placanjeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faktura_id", nullable = false)
    private Faktura faktura;

    @OneToMany(mappedBy = "placanje", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<com.eventsystem.event_management_system.model.Refundacija> refundacije = new java.util.ArrayList<>();

    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal iznos;

    @Enumerated(EnumType.STRING)
    @Column(name = "metod", length = 20)
    private MetodPlacanja metod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private PlacanjeStatus status;

    @Column(columnDefinition = "TEXT")
    private String napomena;

    @Column(name = "kreirano_at", nullable = false)
    private LocalDateTime kreiranoAt;

    @Column(name = "potvrdjeno_at")
    private LocalDateTime potvrdjenoAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = PlacanjeStatus.PENDING;
        if (kreiranoAt == null) kreiranoAt = LocalDateTime.now();
    }
}
