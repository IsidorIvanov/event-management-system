package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.RefundacijaStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@Table(name = "refundacija")
@NoArgsConstructor
@AllArgsConstructor
public class Refundacija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refundacija_id")
    private Long refundacijaId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placanje_id", nullable = false)
    private Placanje placanje;

    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal iznos;

    @Column(columnDefinition = "TEXT")
    private String razlog;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private RefundacijaStatus status;

    @Column(name = "kreirao_id")
    private Long kreiraoId;

    @Column(name = "odobrio_id")
    private Long odobrioId;

    @Column(name = "izvrseno_at")
    private LocalDateTime izvrsenoAt;

    @Column(name = "kreirano_at", nullable = false)
    private LocalDateTime kreiranoAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = RefundacijaStatus.TRAZENA;
        if (kreiranoAt == null) kreiranoAt = LocalDateTime.now();
    }
}
