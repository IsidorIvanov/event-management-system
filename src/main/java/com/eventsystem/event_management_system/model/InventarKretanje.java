package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.TipInventarKretanja;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@Table(name = "inventar_kretanje")
@NoArgsConstructor
@AllArgsConstructor
public class InventarKretanje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kretanje_id")
    private Long kretanjeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oprema_id", nullable = false)
    private Oprema oprema;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipInventarKretanja tip;

    @Column(nullable = false)
    private Integer kolicina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "porudzbenica_id")
    private Porudzbenica porudzbenica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dodela_id")
    private DodelaOpreme dodela;

    @Column(name = "kreirao_id")
    private Long kreiraoId;

    @Column(name = "kreiran_at", nullable = false)
    private LocalDateTime kreiranAt;

    @Column(columnDefinition = "TEXT")
    private String napomena;

    @PrePersist
    protected void onCreate() {
        if (kreiranAt == null) {
            kreiranAt = LocalDateTime.now();
        }
    }
}
