package com.eventsystem.event_management_system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Instalira proceduralni sloj za modul REGISTRACIJA (funkcija, procedure, trigeri)
 * pri pokretanju aplikacije. Isti obrazac kao {@code DobavljacSchemaMigration}
 * (@EventListener(ApplicationReadyEvent) + JdbcTemplate), jer projekat koristi
 * ddl-auto=update bez Flyway/Liquibase-a.
 *
 * VAZNO: Svaki objekat se izvrsava kao JEDNA JDBC naredba, pa se NE koristi
 * DELIMITER (to je direktiva mysql klijenta, ne JDBC-a). Tacke-zapete unutar
 * tela procedure/trigera su deo jedne CREATE naredbe i ne razdvajaju je.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistracijaProceduralMigration {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void install() {
        try {
            // Redosled: prvo obrisi (idempotentno), pa kreiraj.
            jdbcTemplate.execute("DROP FUNCTION IF EXISTS fn_reg_slobodna_mesta");
            jdbcTemplate.execute(FN_SLOBODNA_MESTA);

            jdbcTemplate.execute("DROP PROCEDURE IF EXISTS sp_reg_prijavi");
            jdbcTemplate.execute(SP_PRIJAVI);

            jdbcTemplate.execute("DROP PROCEDURE IF EXISTS sp_reg_otkazi");
            jdbcTemplate.execute(SP_OTKAZI);

            jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_registracija_bi");
            jdbcTemplate.execute(TRG_REGISTRACIJA_BI);

            jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_dogadjaj_au_promocija");
            jdbcTemplate.execute(TRG_DOGADJAJ_PROMOCIJA);

            log.info("Proceduralni sloj za registracije je instaliran (fn/sp/trigeri).");
        } catch (Exception ex) {
            log.error("Instalacija proceduralnog sloja za registracije nije uspela: {}", ex.getMessage(), ex);
        }
    }

    // --- Definicije bez DELIMITER-a (svaka je jedna CREATE naredba) ---------

    private static final String FN_SLOBODNA_MESTA = """
            CREATE FUNCTION fn_reg_slobodna_mesta(p_dogadjaj_id BIGINT)
                RETURNS INT
                READS SQL DATA
            BEGIN
                DECLARE v_kapacitet   INT;
                DECLARE v_potvrdjenih INT;

                SELECT maks_kapacitet INTO v_kapacitet
                FROM dogadjaj WHERE dogadjaj_id = p_dogadjaj_id;

                IF v_kapacitet IS NULL THEN
                    RETURN NULL;
                END IF;

                SELECT COUNT(*) INTO v_potvrdjenih
                FROM registracija
                WHERE dogadjaj_id = p_dogadjaj_id AND status = 'POTVRDJENA';

                RETURN v_kapacitet - v_potvrdjenih;
            END
            """;

    private static final String SP_PRIJAVI = """
            CREATE PROCEDURE sp_reg_prijavi(
                IN  p_korisnik_id      BIGINT,
                IN  p_dogadjaj_id      BIGINT,
                IN  p_naziv_tipa       VARCHAR(100),
                OUT p_registracija_id  BIGINT,
                OUT p_status           VARCHAR(20)
            )
            proc: BEGIN
                DECLARE v_kapacitet    INT;
                DECLARE v_status_dog   VARCHAR(20);
                DECLARE v_ima_tip      INT;
                DECLARE v_potvrdjenih  INT;
                DECLARE v_na_cekanju   BOOLEAN;
                DECLARE v_status_karte VARCHAR(20);

                DECLARE EXIT HANDLER FOR SQLEXCEPTION
                BEGIN
                    ROLLBACK;
                    RESIGNAL;
                END;

                START TRANSACTION;

                SELECT maks_kapacitet, status
                INTO   v_kapacitet, v_status_dog
                FROM   dogadjaj
                WHERE  dogadjaj_id = p_dogadjaj_id
                FOR UPDATE;

                IF v_kapacitet IS NULL THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Dogadjaj nije pronadjen.';
                END IF;

                IF v_status_dog = 'ZAVRSEN' THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Prijave za ovaj dogadjaj su zatvorene.';
                END IF;

                SELECT COUNT(*) INTO v_ima_tip
                FROM   tip_karte
                WHERE  dogadjaj_id = p_dogadjaj_id
                  AND  naziv_tipa = p_naziv_tipa COLLATE utf8mb4_unicode_ci;

                IF v_ima_tip = 0 THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tip karte nije pronadjen.';
                END IF;

                IF EXISTS (
                    SELECT 1 FROM registracija
                    WHERE korisnik_id = p_korisnik_id
                      AND dogadjaj_id = p_dogadjaj_id
                      AND status <> 'OTKAZANA'
                ) THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vec ste registrovani na ovaj dogadjaj.';
                END IF;

                SELECT COUNT(*) INTO v_potvrdjenih
                FROM   registracija
                WHERE  dogadjaj_id = p_dogadjaj_id AND status = 'POTVRDJENA';

                SET v_na_cekanju   = (v_potvrdjenih >= v_kapacitet);
                SET p_status       = IF(v_na_cekanju, 'NA_CEKANJU', 'POTVRDJENA');
                SET v_status_karte = IF(v_na_cekanju, 'NA_CEKANJU', 'VALIDNA');

                INSERT INTO registracija (korisnik_id, dogadjaj_id, naziv_tipa, status, status_karte)
                VALUES (p_korisnik_id, p_dogadjaj_id, p_naziv_tipa, p_status, v_status_karte);

                SET p_registracija_id = LAST_INSERT_ID();

                UPDATE registracija
                SET    broj_karte = CONCAT('KT-', p_registracija_id, '-', p_dogadjaj_id)
                WHERE  registracija_id = p_registracija_id;

                COMMIT;
            END
            """;

    private static final String SP_OTKAZI = """
            CREATE PROCEDURE sp_reg_otkazi(
                IN  p_registracija_id BIGINT,
                IN  p_korisnik_id     BIGINT,
                OUT p_promovisan_id   BIGINT
            )
            proc: BEGIN
                DECLARE v_dogadjaj_id     BIGINT DEFAULT NULL;
                DECLARE v_vlasnik_id      BIGINT DEFAULT NULL;
                DECLARE v_status          VARCHAR(20);
                DECLARE v_bilo_potvrdjeno BOOLEAN DEFAULT FALSE;
                DECLARE v_kapacitet       INT;
                DECLARE v_potvrdjenih     INT;
                DECLARE v_nema_reda       INT DEFAULT 0;

                DECLARE EXIT HANDLER FOR SQLEXCEPTION
                BEGIN
                    ROLLBACK;
                    RESIGNAL;
                END;
                DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_nema_reda = 1;

                SET p_promovisan_id = NULL;

                START TRANSACTION;

                SELECT dogadjaj_id, korisnik_id, status
                INTO   v_dogadjaj_id, v_vlasnik_id, v_status
                FROM   registracija
                WHERE  registracija_id = p_registracija_id
                FOR UPDATE;

                IF v_dogadjaj_id IS NULL THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Registracija nije pronadjena.';
                END IF;

                IF v_vlasnik_id <> p_korisnik_id THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Nemate pravo da otkazete ovu registraciju.';
                END IF;

                IF v_status = 'OTKAZANA' THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Registracija je vec otkazana.';
                END IF;

                SET v_bilo_potvrdjeno = (v_status = 'POTVRDJENA');

                UPDATE registracija
                SET    status = 'OTKAZANA', status_karte = 'NEVAZECA'
                WHERE  registracija_id = p_registracija_id;

                IF v_bilo_potvrdjeno THEN
                    SELECT maks_kapacitet INTO v_kapacitet
                    FROM   dogadjaj WHERE dogadjaj_id = v_dogadjaj_id FOR UPDATE;

                    SELECT COUNT(*) INTO v_potvrdjenih
                    FROM   registracija
                    WHERE  dogadjaj_id = v_dogadjaj_id AND status = 'POTVRDJENA';

                    IF v_kapacitet IS NULL OR v_potvrdjenih < v_kapacitet THEN
                        SET v_nema_reda = 0;
                        SELECT registracija_id INTO p_promovisan_id
                        FROM   registracija
                        WHERE  dogadjaj_id = v_dogadjaj_id AND status = 'NA_CEKANJU'
                        ORDER  BY registracija_id ASC
                        LIMIT 1
                        FOR UPDATE;

                        IF v_nema_reda = 1 THEN
                            SET p_promovisan_id = NULL;
                        ELSE
                            UPDATE registracija
                            SET    status = 'POTVRDJENA', status_karte = 'VALIDNA'
                            WHERE  registracija_id = p_promovisan_id;
                        END IF;
                    END IF;
                END IF;

                COMMIT;
            END
            """;

    private static final String TRG_REGISTRACIJA_BI = """
            CREATE TRIGGER trg_registracija_bi
            BEFORE INSERT ON registracija
            FOR EACH ROW
            BEGIN
                IF NEW.vreme_registracije IS NULL THEN
                    SET NEW.vreme_registracije = NOW();
                END IF;
                IF NEW.datum_registracije IS NULL THEN
                    SET NEW.datum_registracije = DATE(NEW.vreme_registracije);
                END IF;
                IF NEW.status IS NULL THEN
                    SET NEW.status = 'POTVRDJENA';
                END IF;
                IF NEW.status_karte IS NULL THEN
                    SET NEW.status_karte = 'VALIDNA';
                END IF;
            END
            """;

    private static final String TRG_DOGADJAJ_PROMOCIJA = """
            CREATE TRIGGER trg_dogadjaj_au_promocija
            AFTER UPDATE ON dogadjaj
            FOR EACH ROW
            BEGIN
                DECLARE v_potvrdjenih INT;
                DECLARE v_slobodno    INT;
                DECLARE v_reg_id      BIGINT;
                DECLARE v_kraj        INT DEFAULT 0;

                DECLARE c_cekanje CURSOR FOR
                    SELECT registracija_id
                    FROM   registracija
                    WHERE  dogadjaj_id = NEW.dogadjaj_id AND status = 'NA_CEKANJU'
                    ORDER  BY registracija_id ASC;

                DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_kraj = 1;

                IF NEW.maks_kapacitet > OLD.maks_kapacitet
                   AND NEW.status IN ('OBJAVLJEN', 'AKTIVAN') THEN

                    SELECT COUNT(*) INTO v_potvrdjenih
                    FROM   registracija
                    WHERE  dogadjaj_id = NEW.dogadjaj_id AND status = 'POTVRDJENA';

                    SET v_slobodno = NEW.maks_kapacitet - v_potvrdjenih;

                    IF v_slobodno > 0 THEN
                        OPEN c_cekanje;
                        promo: LOOP
                            FETCH c_cekanje INTO v_reg_id;
                            IF v_kraj = 1 OR v_slobodno <= 0 THEN
                                LEAVE promo;
                            END IF;

                            UPDATE registracija
                            SET    status = 'POTVRDJENA', status_karte = 'VALIDNA'
                            WHERE  registracija_id = v_reg_id;

                            SET v_slobodno = v_slobodno - 1;
                        END LOOP;
                        CLOSE c_cekanje;
                    END IF;
                END IF;
            END
            """;
}