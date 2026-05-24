package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.StatusUgovora;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@Table(name = "ugovor")
@NoArgsConstructor
@AllArgsConstructor
public class Ugovor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ugovor_id")
    private Long ugovorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dobavljac_id", nullable = false)
    private Dobavljac dobavljac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nabavka_id")
    private Nabavka nabavka;

    @Column(name = "dokument_url", length = 500)
    private String dokumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusUgovora status;

    @Column(name = "kreiran_at", nullable = false)
    private LocalDateTime kreiranAt;

    @Column(name = "vazi_do")
    private LocalDate vaziDo;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = StatusUgovora.NACRT;
        }
        if (kreiranAt == null) {
            kreiranAt = LocalDateTime.now();
        }
    }
}
