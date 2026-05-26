package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.dto.BudzetAlertDto;
import com.eventsystem.event_management_system.utils.enums.AlertLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BudzetAlertServiceTest {

    private final BudzetAlertService service = new BudzetAlertService();

    @Test
    void evaluateOnly_returnsExpectedLevelsForThresholds() {
        assertThat(service.evaluateOnly(
                new BigDecimal("125.00"),
                new BigDecimal("100.00"),
                new BigDecimal("0.8000"),
                new BigDecimal("0.9500")
        )).isEqualTo(AlertLevel.EXCEEDED);

        assertThat(service.evaluateOnly(
                new BigDecimal("85.00"),
                new BigDecimal("100.00"),
                new BigDecimal("0.8000"),
                new BigDecimal("0.9500")
        )).isEqualTo(AlertLevel.WARNING);

        assertThat(service.evaluateOnly(
                new BigDecimal("97.00"),
                new BigDecimal("100.00"),
                new BigDecimal("0.8000"),
                new BigDecimal("0.9500")
        )).isEqualTo(AlertLevel.CRITICAL);
    }

    @Test
    void evaluateOnly_handlesZeroPlanAsExceededWhenThereIsCost() {
        assertThat(service.evaluateOnly(
                new BigDecimal("1.00"),
                BigDecimal.ZERO,
                new BigDecimal("0.8000"),
                new BigDecimal("0.9500")
        )).isEqualTo(AlertLevel.EXCEEDED);
    }

    @Test
    void evaluateAndRemember_emitsOnlyWhenLevelIncreasesAndRemembersDrops() {
        BudzetAlertDto firstExceeded = service.evaluateAndRemember(
                1L,
                2L,
                "Catering",
                new BigDecimal("125.00"),
                new BigDecimal("100.00"),
                new BigDecimal("0.8000"),
                new BigDecimal("0.9500")
        );
        assertThat(firstExceeded.isNotificationEmitted()).isTrue();

        BudzetAlertDto sameExceeded = service.evaluateAndRemember(
                1L,
                2L,
                "Catering",
                new BigDecimal("130.00"),
                new BigDecimal("100.00"),
                new BigDecimal("0.8000"),
                new BigDecimal("0.9500")
        );
        assertThat(sameExceeded.isNotificationEmitted()).isFalse();

        BudzetAlertDto droppedToNone = service.evaluateAndRemember(
                1L,
                2L,
                "Catering",
                new BigDecimal("70.00"),
                new BigDecimal("100.00"),
                new BigDecimal("0.8000"),
                new BigDecimal("0.9500")
        );
        assertThat(droppedToNone.getAlertLevel()).isEqualTo(AlertLevel.NONE);
        assertThat(droppedToNone.isNotificationEmitted()).isFalse();

        BudzetAlertDto warningAfterDrop = service.evaluateAndRemember(
                1L,
                2L,
                "Catering",
                new BigDecimal("85.00"),
                new BigDecimal("100.00"),
                new BigDecimal("0.8000"),
                new BigDecimal("0.9500")
        );
        assertThat(warningAfterDrop.getAlertLevel()).isEqualTo(AlertLevel.WARNING);
        assertThat(warningAfterDrop.isNotificationEmitted()).isTrue();
    }
}
