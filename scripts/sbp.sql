-- --------------------------------------------------------------------
-- 1) FUNKCIJA
DROP FUNCTION IF EXISTS fn_reg_slobodna_mesta;
DELIMITER $$
CREATE FUNCTION fn_reg_slobodna_mesta(p_dogadjaj_id BIGINT)
    RETURNS INT
    READS SQL DATA
BEGIN
    DECLARE v_kapacitet   INT;
    DECLARE v_potvrdjenih INT;

    SELECT maks_kapacitet INTO v_kapacitet
    FROM dogadjaj
    WHERE dogadjaj_id = p_dogadjaj_id;

    IF v_kapacitet IS NULL THEN
    RETURN NULL;
END IF;

SELECT COUNT(*) INTO v_potvrdjenih
FROM registracija
WHERE dogadjaj_id = p_dogadjaj_id
  AND status = 'POTVRDJENA';

RETURN v_kapacitet - v_potvrdjenih;
END$$
DELIMITER ;


-- --------------------------------------------------------------------
-- 2) PROCEDURA
DROP PROCEDURE IF EXISTS sp_reg_prijavi;
DELIMITER $$
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

        -- Na bilo koju gresku: ponisti sve i prosledi gresku pozivaocu.
        DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- (1) Zakljucaj red dogadjaja. Drugi konkurentni poziv ceka ovde.
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

    -- (2) Tip karte mora da postoji za taj dogadjaj.
    SELECT COUNT(*) INTO v_ima_tip
    FROM   tip_karte
    WHERE  dogadjaj_id = p_dogadjaj_id
      AND  naziv_tipa  = p_naziv_tipa;

    IF v_ima_tip = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tip karte nije pronadjen.';
    END IF;

    -- (3) Nema duple prijave na isti dogadjaj (otkazane se ignorisu).
    IF EXISTS (
        SELECT 1 FROM registracija
        WHERE korisnik_id = p_korisnik_id
          AND dogadjaj_id = p_dogadjaj_id
          AND status <> 'OTKAZANA'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vec ste registrovani na ovaj dogadjaj.';
    END IF;

    -- (4) Odluka o kapacitetu (lock i dalje drzi red dogadjaja).
    SELECT COUNT(*) INTO v_potvrdjenih
    FROM   registracija
    WHERE  dogadjaj_id = p_dogadjaj_id
      AND  status = 'POTVRDJENA';

    SET v_na_cekanju   = (v_potvrdjenih >= v_kapacitet);
    SET p_status       = IF(v_na_cekanju, 'NA_CEKANJU', 'POTVRDJENA');
    SET v_status_karte = IF(v_na_cekanju, 'NA_CEKANJU', 'VALIDNA');

    -- (5) Unos. Datum/vreme popunjava triger trg_registracija_bi.
    INSERT INTO registracija (korisnik_id, dogadjaj_id, naziv_tipa, status, status_karte)
    VALUES (p_korisnik_id, p_dogadjaj_id, p_naziv_tipa, p_status, v_status_karte);

    SET p_registracija_id = LAST_INSERT_ID();

    -- (6) Broj karte iz tek dobijenog ID-a, u istoj transakciji.
    UPDATE registracija
    SET    broj_karte = CONCAT('KT-', p_registracija_id, '-', p_dogadjaj_id)
    WHERE  registracija_id = p_registracija_id;

    COMMIT;
    END$$
        DELIMITER ;


    -- --------------------------------------------------------------------
-- 3) PROCEDURA
    DROP PROCEDURE IF EXISTS sp_reg_otkazi;
    DELIMITER $$
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
        -- Za SELECT ... INTO koji ne vrati red (nema nikoga na listi cekanja).
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_nema_reda = 1;

        SET p_promovisan_id = NULL;

        START TRANSACTION;

        -- Zakljucaj red registracije koji se otkazuje.
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

        -- Ako se oslobodilo potvrdjeno mesto, promovisi prvog sa liste cekanja.
        IF v_bilo_potvrdjeno THEN
            SELECT maks_kapacitet INTO v_kapacitet
            FROM   dogadjaj
            WHERE  dogadjaj_id = v_dogadjaj_id
                FOR UPDATE;

            SELECT COUNT(*) INTO v_potvrdjenih
            FROM   registracija
            WHERE  dogadjaj_id = v_dogadjaj_id
              AND  status = 'POTVRDJENA';

            IF v_kapacitet IS NULL OR v_potvrdjenih < v_kapacitet THEN
                SET v_nema_reda = 0;
                SELECT registracija_id INTO p_promovisan_id
                FROM   registracija
                WHERE  dogadjaj_id = v_dogadjaj_id
                  AND  status = 'NA_CEKANJU'
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
        END$$
            DELIMITER ;


        -- --------------------------------------------------------------------
-- 4) TRIGER
        DROP TRIGGER IF EXISTS trg_registracija_bi;
        DELIMITER $$
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
            END$$
                DELIMITER ;


            -- --------------------------------------------------------------------
-- 5) TRIGER
            DROP TRIGGER IF EXISTS trg_dogadjaj_au_promocija;
            DELIMITER $$
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
                WHERE  dogadjaj_id = NEW.dogadjaj_id
                AND  status = 'NA_CEKANJU'
                    ORDER  BY registracija_id ASC;

                DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_kraj = 1;

                IF NEW.maks_kapacitet > OLD.maks_kapacitet
                AND NEW.status IN ('OBJAVLJEN', 'AKTIVAN') THEN

                SELECT COUNT(*) INTO v_potvrdjenih
                FROM   registracija
                WHERE  dogadjaj_id = NEW.dogadjaj_id
                AND  status = 'POTVRDJENA';

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
END$$
DELIMITER ;