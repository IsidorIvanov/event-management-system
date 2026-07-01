-- Seeder: registruje učesnike na GamesCon i dodaje ih u raspored sesija 18 i 26.
--   * 20 učesnika u sesiju 18, 20 učesnika u sesiju 26 (40 distinktnih korisnika).
--   * Svaki učesnik se registruje na događaj (registracija) sa RAZLIČITIM vreme_registracije
--     vrednostima, razmaknutim po satima/danima — da satni izveštaj "Attendance Rate"
--     ima smislenu krivu, a "Engagement / Session" da pokaže popunjenost obe sesije.
--
-- Pokretanje: mysql -u root -p event_management_db < scripts/seed-gamescon-registrations.sql
--
-- Idempotentno: prethodno seedovani korisnici (email 'seed.gamescon.%') se brišu na početku.

USE event_management_db;

DROP PROCEDURE IF EXISTS seed_gamescon_registrations;

DELIMITER $$

CREATE PROCEDURE seed_gamescon_registrations()
BEGIN
    DECLARE v_i INT DEFAULT 1;
    DECLARE v_total INT DEFAULT 40;            -- 20 + 20
    DECLARE v_per_session INT DEFAULT 20;
    DECLARE v_sesija BIGINT;
    DECLARE v_event BIGINT;
    DECLARE v_tip VARCHAR(100);
    DECLARE v_kid BIGINT;
    DECLARE v_vreme DATETIME;

    -- ── Razrešavanje događaja i tipa karte iz sesija 18 i 26 ───────────────────
    SET @ev18 = (SELECT dogadjaj_id FROM sesija WHERE sesija_id = 18 LIMIT 1);
    SET @ev26 = (SELECT dogadjaj_id FROM sesija WHERE sesija_id = 26 LIMIT 1);

    IF @ev18 IS NULL OR @ev26 IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Sesija 18 i/ili 26 ne postoji — proverite ID-jeve.';
    END IF;

    -- Obezbedi tip karte (registracija ima FK na tip_karte preko (dogadjaj_id, naziv_tipa)).
    -- Ako događaj nema nijedan tip karte, kreiraj seed VISEDNEVNA kartu.
    SET @tip18 = (SELECT naziv_tipa FROM tip_karte WHERE dogadjaj_id = @ev18 ORDER BY naziv_tipa LIMIT 1);
    IF @tip18 IS NULL THEN
        INSERT INTO tip_karte (dogadjaj_id, naziv_tipa, vrsta, cena, kvota, opis)
        VALUES (@ev18, 'Seed Pass', 'VISEDNEVNA', 0.00, 1000, 'Seed tip karte (auto-kreiran)');
        SET @tip18 = 'Seed Pass';
    END IF;

    SET @tip26 = (SELECT naziv_tipa FROM tip_karte WHERE dogadjaj_id = @ev26 ORDER BY naziv_tipa LIMIT 1);
    IF @tip26 IS NULL THEN
        INSERT INTO tip_karte (dogadjaj_id, naziv_tipa, vrsta, cena, kvota, opis)
        VALUES (@ev26, 'Seed Pass', 'VISEDNEVNA', 0.00, 1000, 'Seed tip karte (auto-kreiran)');
        SET @tip26 = 'Seed Pass';
    END IF;

    -- ── Petlja: kreiraj korisnike i registruj ih ──────────────────────────────
    WHILE v_i <= v_total DO
        IF v_i <= v_per_session THEN
            SET v_sesija = 18;  SET v_event = @ev18;  SET v_tip = @tip18;
        ELSE
            SET v_sesija = 26;  SET v_event = @ev26;  SET v_tip = @tip26;
        END IF;

        -- Različito vreme registracije: kreni od 2026-06-20 08:00 i pomeraj po 97 min
        -- (≈ 40 distinktnih sati raspoređenih preko ~2.7 dana).
        SET v_vreme = DATE_ADD('2026-06-20 08:00:00', INTERVAL (v_i - 1) * 97 MINUTE);

        -- 1) Korisnik (bazna tabela JOINED nasleđivanja) + diskriminator UCESNIK
        INSERT INTO korisnik (ime, prezime, email, lozinka_hash, telefon, datum_registracije, status, tip_korisnika)
        VALUES (
            CONCAT('Gost', v_i),
            'GamesCon',
            CONCAT('seed.gamescon.user', v_i, '@example.com'),
            -- bcrypt hash za lozinku "password" (samo za seed; nije za produkciju)
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            CONCAT('06', LPAD(v_i, 7, '0')),
            DATE(v_vreme),
            'AKTIVAN',
            'UCESNIK'
        );
        SET v_kid = LAST_INSERT_ID();

        -- 2) Ucesnik (pod-tabela JOINED nasleđivanja)
        INSERT INTO ucesnik (korisnik_id, kompanija, pozicija, datum_rodjenja)
        VALUES (v_kid, 'Seed Co.', 'Posetilac', DATE_ADD('1990-01-01', INTERVAL v_i DAY));

        -- 3) Registracija na događaj (nosi vreme_registracije za satni izveštaj)
        INSERT INTO registracija (korisnik_id, dogadjaj_id, naziv_tipa, datum_registracije, vreme_registracije, status, broj_karte, qr_kod, status_karte)
        VALUES (
            v_kid, v_event, v_tip,
            DATE(v_vreme), v_vreme,
            'POTVRDJENA',
            CONCAT('KT-GC-', v_i, '-', v_event),
            NULL,
            'VALIDNA'
        );

        -- 4) Dodavanje sesije u raspored učesnika (popunjenost po sesiji)
        INSERT INTO ucesnik_sesija (korisnik_id, sesija_id, datum_dodavanja)
        VALUES (v_kid, v_sesija, v_vreme);

        SET v_i = v_i + 1;
    END WHILE;
END$$

DELIMITER ;

-- ── Čišćenje prethodnog seeda (FK-safe redosled) ──────────────────────────────
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

DELETE us FROM ucesnik_sesija us
  JOIN korisnik k ON us.korisnik_id = k.korisnik_id
  WHERE k.email LIKE 'seed.gamescon.%';

DELETE r FROM registracija r
  JOIN korisnik k ON r.korisnik_id = k.korisnik_id
  WHERE k.email LIKE 'seed.gamescon.%';

DELETE ui FROM ucesnik_interes ui
  JOIN korisnik k ON ui.korisnik_id = k.korisnik_id
  WHERE k.email LIKE 'seed.gamescon.%';

DELETE u FROM ucesnik u
  JOIN korisnik k ON u.korisnik_id = k.korisnik_id
  WHERE k.email LIKE 'seed.gamescon.%';

DELETE FROM korisnik WHERE email LIKE 'seed.gamescon.%';

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- ── Izvrši seeder ─────────────────────────────────────────────────────────────
CALL seed_gamescon_registrations();
DROP PROCEDURE IF EXISTS seed_gamescon_registrations;

-- ── Provera ───────────────────────────────────────────────────────────────────
SELECT 'Seedovani korisnici:' AS info, COUNT(*) AS broj
FROM korisnik WHERE email LIKE 'seed.gamescon.%';

SELECT 'Registracije po događaju (sesija 18):' AS info, COUNT(*) AS broj
FROM registracija r JOIN korisnik k ON r.korisnik_id = k.korisnik_id
WHERE k.email LIKE 'seed.gamescon.%'
  AND r.dogadjaj_id = (SELECT dogadjaj_id FROM sesija WHERE sesija_id = 18);

SELECT 'U rasporedu sesije 18:' AS info, COUNT(*) AS broj
FROM ucesnik_sesija WHERE sesija_id = 18;

SELECT 'U rasporedu sesije 26:' AS info, COUNT(*) AS broj
FROM ucesnik_sesija WHERE sesija_id = 26;

SELECT 'Uzorak vremena registracije:' AS info;
SELECT k.email, r.vreme_registracije
FROM registracija r JOIN korisnik k ON r.korisnik_id = k.korisnik_id
WHERE k.email LIKE 'seed.gamescon.%'
ORDER BY r.vreme_registracije
LIMIT 10;
