package com.eventsystem.event_management_system.plsql.oracle.budzet;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class BudzetUpozorenjeDto {

    Long kategorijaId;
    String kategorijaNaziv;
    BigDecimal planiraniIznos;
    BigDecimal stvarniIznos;
    String statusKontrole;
    String alertNivo;
    BigDecimal ratio;
}
