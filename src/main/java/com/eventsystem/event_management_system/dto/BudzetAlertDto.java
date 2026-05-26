package com.eventsystem.event_management_system.dto;

import com.eventsystem.event_management_system.utils.enums.AlertLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudzetAlertDto {

    private Long budzetId;

    private Long kategorijaId;

    private String kategorijaNaziv;

    private AlertLevel alertLevel;

    private BigDecimal iskoriscenost;

    private String poruka;

    private boolean notificationEmitted;
}
