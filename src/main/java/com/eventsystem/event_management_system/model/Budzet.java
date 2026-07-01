package com.eventsystem.event_management_system.model;

import com.eventsystem.event_management_system.utils.enums.BudzetStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Getter
@Setter
@Table(name = "budzet")
@Check(constraints = "planirani_iznos >= 0 and odobreni_iznos >= 0")
@NoArgsConstructor
@AllArgsConstructor
public class Budzet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budzet_id")
    private Long budzetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dogadjaj_id", nullable = false)
    private Dogadjaj dogadjaj;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kreirao_id", nullable = false)
    private Zaposleni kreirao;

    @Column(name = "naziv_budzeta", nullable = false, length = 255)
    private String nazivBudzeta;

    @Column(name = "planirani_iznos", nullable = false, precision = 15, scale = 2)
    private BigDecimal planiraniIznos;

    @Column(name = "odobreni_iznos", nullable = false, precision = 15, scale = 2)
    private BigDecimal odobreniIznos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BudzetStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "zatvoren_at")
    private LocalDateTime zatvorenAt;

    @OneToMany(mappedBy = "budzet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StavkaBudzeta> stavke = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = BudzetStatus.DRAFT;
        }
        if (odobreniIznos == null) {
            odobreniIznos = planiraniIznos;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
