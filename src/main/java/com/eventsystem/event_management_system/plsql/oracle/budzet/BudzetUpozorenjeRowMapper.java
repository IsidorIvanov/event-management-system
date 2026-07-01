package com.eventsystem.event_management_system.plsql.oracle.budzet;

import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BudzetUpozorenjeRowMapper implements RowMapper<BudzetUpozorenjeDto> {

    @Override
    public BudzetUpozorenjeDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        return BudzetUpozorenjeDto.builder()
                .kategorijaId(rs.getLong("KATEGORIJA_ID"))
                .kategorijaNaziv(rs.getString("KATEGORIJA_NAZIV"))
                .planiraniIznos(rs.getBigDecimal("PLANIRANI_IZNOS"))
                .stvarniIznos(rs.getBigDecimal("STVARNI_IZNOS"))
                .statusKontrole(rs.getString("STATUS_KONTROLE"))
                .alertNivo(rs.getString("ALERT_NIVO"))
                .ratio(rs.getBigDecimal("RATIO"))
                .build();
    }
}
