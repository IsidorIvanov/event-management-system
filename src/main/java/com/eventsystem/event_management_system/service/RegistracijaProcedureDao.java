package com.eventsystem.event_management_system.service;

import com.eventsystem.event_management_system.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

/**
 * Tanak sloj koji poziva uskladistene procedure registracija modula
 * (sp_reg_prijavi, sp_reg_otkazi) i prevodi greske iz baze.
 *
 * Procedure same upravljaju transakcijom (START TRANSACTION / COMMIT / ROLLBACK),
 * pa se pozivi NE obavijaju u Spring @Transactional. Greske koje procedura
 * podigne preko SIGNAL SQLSTATE '45000' stizu kao SQLException (SQLState 45000)
 * i ovde se prevode u {@link BadRequestException} (HTTP 400) sa porukom iz baze.
 */
@Component
@RequiredArgsConstructor
public class RegistracijaProcedureDao {

    private static final String SQLSTATE_APP_ERROR = "45000";

    private final JdbcTemplate jdbcTemplate;

    private SimpleJdbcCall prijaviCall;
    private SimpleJdbcCall otkaziCall;

    @PostConstruct
    void init() {
        this.prijaviCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_reg_prijavi")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_korisnik_id", Types.BIGINT),
                        new SqlParameter("p_dogadjaj_id", Types.BIGINT),
                        new SqlParameter("p_naziv_tipa", Types.VARCHAR),
                        new SqlOutParameter("p_registracija_id", Types.BIGINT),
                        new SqlOutParameter("p_status", Types.VARCHAR)
                );

        this.otkaziCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_reg_otkazi")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_registracija_id", Types.BIGINT),
                        new SqlParameter("p_korisnik_id", Types.BIGINT),
                        new SqlOutParameter("p_promovisan_id", Types.BIGINT)
                );
    }

    /** Rezultat prijave: ID nove registracije i njen finalni status. */
    public record PrijavaRezultat(Long registracijaId, String status) {}

    public PrijavaRezultat prijavi(Long korisnikId, Long dogadjajId, String nazivTipa) {
        try {
            Map<String, Object> out = prijaviCall.execute(new MapSqlParameterSource()
                    .addValue("p_korisnik_id", korisnikId)
                    .addValue("p_dogadjaj_id", dogadjajId)
                    .addValue("p_naziv_tipa", nazivTipa));

            Long regId = out.get("p_registracija_id") == null
                    ? null : ((Number) out.get("p_registracija_id")).longValue();
            String status = (String) out.get("p_status");
            return new PrijavaRezultat(regId, status);
        } catch (DataAccessException ex) {
            throw prevediGresku(ex);
        }
    }

    /** Otkazuje registraciju; vraca ID promovisanog sa liste cekanja ili {@code null}. */
    public Long otkazi(Long registracijaId, Long korisnikId) {
        try {
            Map<String, Object> out = otkaziCall.execute(new MapSqlParameterSource()
                    .addValue("p_registracija_id", registracijaId)
                    .addValue("p_korisnik_id", korisnikId));

            Object promovisan = out.get("p_promovisan_id");
            return promovisan == null ? null : ((Number) promovisan).longValue();
        } catch (DataAccessException ex) {
            throw prevediGresku(ex);
        }
    }

    /**
     * Prevodi Spring DataAccessException u citljivu gresku. Ako je uzrok
     * korisnicka greska iz procedure (SIGNAL SQLSTATE '45000'), vraca
     * BadRequestException sa porukom iz baze; inace prosledjuje original.
     */
    private RuntimeException prevediGresku(DataAccessException ex) {
        Throwable t = ex;
        while (t != null && !(t instanceof SQLException)) {
            t = t.getCause();
        }
        if (t instanceof SQLException se && SQLSTATE_APP_ERROR.equals(se.getSQLState())) {
            return new BadRequestException(se.getMessage());
        }
        return ex;
    }
}