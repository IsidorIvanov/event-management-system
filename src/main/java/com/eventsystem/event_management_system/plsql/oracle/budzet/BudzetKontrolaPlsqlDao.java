package com.eventsystem.event_management_system.plsql.oracle.budzet;

import com.eventsystem.event_management_system.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Oracle PL/SQL sidecar za F3 ({@code PKG_BUDZET_KONTROLA}).
 * Ne zamenjuje {@link com.eventsystem.event_management_system.service.BudzetService}
 * niti MySQL tok — aktivno samo kad je {@code app.oracle.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "app.oracle.enabled", havingValue = "true")
public class BudzetKontrolaPlsqlDao {

    /** Oracle {@code OracleTypes.CURSOR} — konstanta da ne zavisi od ojdbc na compile vremenu. */
    private static final int ORACLE_CURSOR_TYPE = -10;

    private final JdbcTemplate oracleJdbcTemplate;

    public BudzetKontrolaPlsqlDao(@Qualifier("oracleJdbcTemplate") JdbcTemplate oracleJdbcTemplate) {
        this.oracleJdbcTemplate = oracleJdbcTemplate;
    }

    private SimpleJdbcCall obradiCall;

    @PostConstruct
    void init() {
        this.obradiCall = new SimpleJdbcCall(oracleJdbcTemplate)
                .withCatalogName("PKG_BUDZET_KONTROLA")
                .withProcedureName("PR_OBRADI_BUDZET")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_BUDZET_ID", Types.NUMERIC),
                        new SqlOutParameter(
                                "P_UPOZORENJA",
                                ORACLE_CURSOR_TYPE,
                                new BudzetUpozorenjeRowMapper()
                        ),
                        new SqlOutParameter("P_BROJ_ALERTA", Types.NUMERIC)
                );
    }

    public record ObradiRezultat(List<BudzetUpozorenjeDto> upozorenja, int brojAlerta) {}

    public ObradiRezultat obradiBudzet(Long budzetId) {
        try {
            Map<String, Object> out = obradiCall.execute(
                    new MapSqlParameterSource("P_BUDZET_ID", budzetId)
            );

            @SuppressWarnings("unchecked")
            List<BudzetUpozorenjeDto> upozorenja = (List<BudzetUpozorenjeDto>) out.get("P_UPOZORENJA");
            Number broj = (Number) out.get("P_BROJ_ALERTA");

            return new ObradiRezultat(
                    upozorenja != null ? upozorenja : List.of(),
                    broj != null ? broj.intValue() : 0
            );
        } catch (DataAccessException ex) {
            throw prevediGresku(ex);
        }
    }

    private RuntimeException prevediGresku(DataAccessException ex) {
        Throwable t = ex;
        while (t != null && !(t instanceof SQLException)) {
            t = t.getCause();
        }
        if (t instanceof SQLException se) {
            int code = se.getErrorCode();
            if (code == 20001 || code == 20002 || code == 20099 || code == 20098) {
                return new BadRequestException(se.getMessage());
            }
        }
        return ex;
    }
}
