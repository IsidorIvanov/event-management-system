package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.BudzetAlertDto;
import com.eventsystem.event_management_system.utils.enums.AlertLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class BudzetAlertService {

    private static final BigDecimal ONE = BigDecimal.ONE;

    /**
     * Pamti poslednji alert nivo po (budzetId, kategorijaId) tokom
     * trajanja aplikacije. Resetuje se pri restartu - prihvatljivo
     * za studentski projekat. U produkciji zameniti Redis TTL kes
     * ili kolonom last_alert_level na stavke_budzeta.
     */
    private final Map<String, AlertLevel> lastAlertLevelMap = new ConcurrentHashMap<>();

    public AlertLevel evaluateOnly(
            BigDecimal stvarniIznos,
            BigDecimal planiraniIznos,
            BigDecimal pragUpozorenja,
            BigDecimal pragKriticnog
    ) {
        BigDecimal stvarni = safe(stvarniIznos);
        BigDecimal planirani = safe(planiraniIznos);
        if (planirani.compareTo(BigDecimal.ZERO) == 0) {
            return stvarni.compareTo(BigDecimal.ZERO) == 0 ? AlertLevel.NONE : AlertLevel.EXCEEDED;
        }

        BigDecimal ratio = ratio(stvarni, planirani);
        if (ratio.compareTo(safe(pragUpozorenja)) < 0) {
            return AlertLevel.NONE;
        }
        if (ratio.compareTo(safe(pragKriticnog)) < 0) {
            return AlertLevel.WARNING;
        }
        if (ratio.compareTo(ONE) < 0) {
            return AlertLevel.CRITICAL;
        }
        return AlertLevel.EXCEEDED;
    }

    public BudzetAlertDto evaluateAndRemember(
            Long budzetId,
            Long kategorijaId,
            String kategorijaNaziv,
            BigDecimal stvarniIznos,
            BigDecimal planiraniIznos,
            BigDecimal pragUpozorenja,
            BigDecimal pragKriticnog
    ) {
        AlertLevel newLevel = evaluateOnly(stvarniIznos, planiraniIznos, pragUpozorenja, pragKriticnog);
        String key = key(budzetId, kategorijaId);
        AlertLevel lastLevel = lastAlertLevelMap.getOrDefault(key, AlertLevel.NONE);
        BigDecimal iskoriscenost = calculateIskoriscenost(stvarniIznos, planiraniIznos);
        boolean emitted = false;

        if (newLevel != lastLevel) {
            if (newLevel.ordinal() > lastLevel.ordinal()) {
                emitAlert(budzetId, kategorijaId, newLevel, iskoriscenost);
                emitted = true;
            }
            lastAlertLevelMap.put(key, newLevel);
        }

        return toDto(budzetId, kategorijaId, kategorijaNaziv, newLevel, iskoriscenost, emitted);
    }

    public BigDecimal calculateIskoriscenost(BigDecimal stvarniIznos, BigDecimal planiraniIznos) {
        BigDecimal stvarni = safe(stvarniIznos);
        BigDecimal planirani = safe(planiraniIznos);
        if (planirani.compareTo(BigDecimal.ZERO) == 0) {
            return stvarni.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_EVEN)
                    : null;
        }
        return stvarni.divide(planirani, 4, RoundingMode.HALF_EVEN);
    }

    public BudzetAlertDto toDto(
            Long budzetId,
            Long kategorijaId,
            String kategorijaNaziv,
            BigDecimal stvarniIznos,
            BigDecimal planiraniIznos,
            BigDecimal pragUpozorenja,
            BigDecimal pragKriticnog
    ) {
        AlertLevel alertLevel = evaluateOnly(stvarniIznos, planiraniIznos, pragUpozorenja, pragKriticnog);
        BigDecimal iskoriscenost = calculateIskoriscenost(stvarniIznos, planiraniIznos);
        return toDto(budzetId, kategorijaId, kategorijaNaziv, alertLevel, iskoriscenost, false);
    }

    private BudzetAlertDto toDto(
            Long budzetId,
            Long kategorijaId,
            String kategorijaNaziv,
            AlertLevel alertLevel,
            BigDecimal iskoriscenost,
            boolean notificationEmitted
    ) {
        return BudzetAlertDto.builder()
                .budzetId(budzetId)
                .kategorijaId(kategorijaId)
                .kategorijaNaziv(kategorijaNaziv)
                .alertLevel(alertLevel)
                .iskoriscenost(iskoriscenost)
                .poruka(buildPoruka(kategorijaNaziv, alertLevel, iskoriscenost))
                .notificationEmitted(notificationEmitted)
                .build();
    }

    private void emitAlert(Long budzetId, Long kategorijaId, AlertLevel alertLevel, BigDecimal ratio) {
        log.info(
                "Budzet alert: budzetId={}, kategorijaId={}, level={}, ratio={}",
                budzetId,
                kategorijaId,
                alertLevel,
                ratio
        );
    }

    private String buildPoruka(String kategorijaNaziv, AlertLevel alertLevel, BigDecimal iskoriscenost) {
        String kategorija = kategorijaNaziv != null ? kategorijaNaziv : "Stavka budzeta";
        if (alertLevel == AlertLevel.NONE) {
            return null;
        }
        if (iskoriscenost == null) {
            return switch (alertLevel) {
                case EXCEEDED -> "%s je prekoracila planirani iznos (plan je 0, postoji trošak).".formatted(kategorija);
                default -> "%s: neplanirani trošak.".formatted(kategorija);
            };
        }
        BigDecimal procenat = iskoriscenost.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_EVEN);
        return switch (alertLevel) {
            case WARNING -> "%s je dostigla prag upozorenja (%s%%).".formatted(kategorija, procenat);
            case CRITICAL -> "%s je dostigla kriticni prag (%s%%).".formatted(kategorija, procenat);
            case EXCEEDED -> "%s je prekoracila planirani iznos (%s%%).".formatted(kategorija, procenat);
            default -> null;
        };
    }

    private BigDecimal ratio(BigDecimal stvarniIznos, BigDecimal planiraniIznos) {
        return stvarniIznos.divide(planiraniIznos, 6, RoundingMode.HALF_EVEN);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String key(Long budzetId, Long kategorijaId) {
        return budzetId + ":" + kategorijaId;
    }
}
