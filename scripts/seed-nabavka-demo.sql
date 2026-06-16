-- Demo podaci: dobavljači, cenovnik, ugovori
-- Pokretanje: mysql -u root -p event_management_db < scripts/seed-nabavka-demo.sql
--
-- PRE USLOV: prvo pokrenite scripts/seed-demo-data.sql (događaji Tech Summit / FinTech Forum).
-- Ako tabela ugovor ima staru šemu, pokrenite Spring Boot jednom (ddl-auto=update)
-- ili sekciju MIGRACIJA ispod.

USE event_management_db;

-- Kolona rejting mora biti opciona (kreiranje dobavljača bez rejtinga)
ALTER TABLE dobavljac MODIFY COLUMN rejting DECIMAL(3,2) NULL;

-- =============================================================================
-- MIGRACIJA: dopuna tabele ugovor (stara šema bez F4 polja)
-- =============================================================================
SET @db = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ugovor' AND COLUMN_NAME = 'broj_ugovora') = 0,
    'ALTER TABLE ugovor ADD COLUMN broj_ugovora VARCHAR(64) NULL AFTER ugovor_id',
    'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ugovor' AND COLUMN_NAME = 'dogadjaj_id') = 0,
    'ALTER TABLE ugovor ADD COLUMN dogadjaj_id BIGINT NULL AFTER dobavljac_id',
    'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ugovor' AND COLUMN_NAME = 'datum_potpisivanja') = 0,
    'ALTER TABLE ugovor ADD COLUMN datum_potpisivanja DATE NULL AFTER nabavka_id',
    'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ugovor' AND COLUMN_NAME = 'vrednost') = 0,
    'ALTER TABLE ugovor ADD COLUMN vrednost DECIMAL(15,2) NULL AFTER datum_potpisivanja',
    'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ugovor' AND COLUMN_NAME = 'predmet') = 0,
    'ALTER TABLE ugovor ADD COLUMN predmet VARCHAR(500) NULL AFTER vrednost',
    'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ugovor' AND COLUMN_NAME = 'uslovi_placanja') = 0,
    'ALTER TABLE ugovor ADD COLUMN uslovi_placanja TEXT NULL AFTER predmet',
    'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'ugovor' AND COLUMN_NAME = 'updated_at') = 0,
    'ALTER TABLE ugovor ADD COLUMN updated_at DATETIME NULL AFTER vazi_do',
    'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- =============================================================================
-- ČIŠĆENJE demo podataka (FK-safe redosled, stara i nova PIB verzija)
-- Workbench safe update mode: privremeno isključen za održavanje seed skripte
-- =============================================================================
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_dobavljac;
CREATE TEMPORARY TABLE tmp_demo_dobavljac (dobavljac_id BIGINT PRIMARY KEY);

INSERT INTO tmp_demo_dobavljac (dobavljac_id)
SELECT dobavljac_id FROM dobavljac
WHERE pib IN ('101234567', '102345678', '103456789', '100001001', '100001002', '100001003');

DROP TEMPORARY TABLE IF EXISTS tmp_demo_ugovor;
CREATE TEMPORARY TABLE tmp_demo_ugovor (ugovor_id BIGINT PRIMARY KEY);

INSERT INTO tmp_demo_ugovor (ugovor_id)
SELECT u.ugovor_id FROM ugovor u
WHERE u.dobavljac_id IN (SELECT dobavljac_id FROM tmp_demo_dobavljac)
   OR (u.broj_ugovora IS NOT NULL AND u.broj_ugovora LIKE 'UG-DEMO-%');

DROP TEMPORARY TABLE IF EXISTS tmp_demo_nabavka;
CREATE TEMPORARY TABLE tmp_demo_nabavka (nabavka_id BIGINT PRIMARY KEY);

INSERT INTO tmp_demo_nabavka (nabavka_id)
SELECT n.nabavka_id FROM nabavka n
WHERE n.dobavljac_id IN (SELECT dobavljac_id FROM tmp_demo_dobavljac);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_faktura;
CREATE TEMPORARY TABLE tmp_demo_faktura (faktura_id BIGINT PRIMARY KEY);

INSERT INTO tmp_demo_faktura (faktura_id)
SELECT f.faktura_id FROM faktura f
WHERE f.dobavljac_id IN (SELECT dobavljac_id FROM tmp_demo_dobavljac)
   OR f.ugovor_id IN (SELECT ugovor_id FROM tmp_demo_ugovor);

-- 1) Refundacije (moraju pre plaćanja)
DELETE r FROM refundacija r
INNER JOIN placanje p ON r.placanje_id = p.placanje_id
INNER JOIN tmp_demo_faktura tf ON p.faktura_id = tf.faktura_id;

-- 2) Plaćanja i stavke faktura
DELETE p FROM placanje p
INNER JOIN tmp_demo_faktura tf ON p.faktura_id = tf.faktura_id;

DELETE sf FROM stavka_fakture sf
INNER JOIN tmp_demo_faktura tf ON sf.faktura_id = tf.faktura_id;

-- 3) Troškovi (odveži demo reference preko PK, ne brišemo ceo trošak)
UPDATE trosak
SET dobavljac_id = NULL
WHERE trosak_id IN (
    SELECT tid FROM (
        SELECT t.trosak_id AS tid
        FROM trosak t
        INNER JOIN tmp_demo_dobavljac td ON t.dobavljac_id = td.dobavljac_id
    ) sub
);

UPDATE trosak
SET faktura_id = NULL, stavka_fakture_id = NULL
WHERE trosak_id IN (
    SELECT tid FROM (
        SELECT t.trosak_id AS tid
        FROM trosak t
        INNER JOIN tmp_demo_faktura tf ON t.faktura_id = tf.faktura_id
    ) sub
);

-- 4) Fakture
DELETE f FROM faktura f
INNER JOIN tmp_demo_faktura tf ON f.faktura_id = tf.faktura_id;

-- 5) Porudžbenice (prvo stavke, pa header)
DELETE sp FROM stavka_porudzbenice sp
INNER JOIN porudzbenica p ON sp.porudzbenica_id = p.porudzbenica_id
WHERE p.dobavljac_id IN (SELECT dobavljac_id FROM tmp_demo_dobavljac)
   OR p.nabavka_id IN (SELECT nabavka_id FROM tmp_demo_nabavka);

DELETE p FROM porudzbenica p
WHERE p.dobavljac_id IN (SELECT dobavljac_id FROM tmp_demo_dobavljac)
   OR p.nabavka_id IN (SELECT nabavka_id FROM tmp_demo_nabavka);

-- 6) Stavke nabavke (pre cenovnika i nabavke)
DELETE sn FROM stavka_nabavke sn
WHERE sn.nabavka_id IN (SELECT nabavka_id FROM tmp_demo_nabavka)
   OR sn.cenovnik_id IN (
       SELECT c.cenovnik_id FROM cenovnik c
       WHERE c.dobavljac_id IN (SELECT dobavljac_id FROM tmp_demo_dobavljac)
   );

-- 7) Ugovori (odveži nabavku pa obriši)
UPDATE ugovor
SET nabavka_id = NULL
WHERE ugovor_id IN (SELECT ugovor_id FROM tmp_demo_ugovor);

DELETE u FROM ugovor u
INNER JOIN tmp_demo_ugovor tu ON u.ugovor_id = tu.ugovor_id;

-- 8) Nabavke
DELETE n FROM nabavka n
INNER JOIN tmp_demo_nabavka tn ON n.nabavka_id = tn.nabavka_id;

-- 9) Cenovnik
DELETE c FROM cenovnik c
WHERE c.dobavljac_id IN (SELECT dobavljac_id FROM tmp_demo_dobavljac);

-- 10) Dobavljači
DELETE d FROM dobavljac d
INNER JOIN tmp_demo_dobavljac td ON d.dobavljac_id = td.dobavljac_id;

DROP TEMPORARY TABLE IF EXISTS tmp_demo_faktura;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_nabavka;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_ugovor;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_dobavljac;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- =============================================================================
-- UBACIVANJE demo podataka
-- =============================================================================
INSERT INTO dobavljac (naziv, grad, kontakt_email, telefon, pib, status, rejting, kriterijumi) VALUES
('Audio Pro d.o.o.', 'Beograd', 'info@audiopro.rs', '011/123-4567', '101234567', 'AKTIVAN', 4.80, 'Brza isporuka audio opreme'),
('Event Light Sistem', 'Novi Sad', 'prodaja@eventlight.rs', '021/987-6543', '102345678', 'AKTIVAN', 4.20, 'Rasveta i bine'),
('Stage Master', 'Sarajevo', 'office@stagemaster.ba', '033/555-0199', '103456789', 'AKTIVAN', 4.50, 'Kompletna scena i mikrofoni');

SET @audio = (SELECT dobavljac_id FROM dobavljac WHERE pib = '101234567' LIMIT 1);
SET @light = (SELECT dobavljac_id FROM dobavljac WHERE pib = '102345678' LIMIT 1);
SET @stage = (SELECT dobavljac_id FROM dobavljac WHERE pib = '103456789' LIMIT 1);
SET @tech_summit = (SELECT dogadjaj_id FROM dogadjaj WHERE naziv = 'Tech Summit Beograd 2026' LIMIT 1);
SET @fintech = (SELECT dogadjaj_id FROM dogadjaj WHERE naziv = 'FinTech Forum Sarajevo' LIMIT 1);

INSERT INTO cenovnik (dobavljac_id, naziv_resursa, opis, jedinica_mere, cena_jedinicna, dostupnost) VALUES
(@audio, 'Zvučnici PA', 'Profesionalni PA sistem', 'komplet', 45000.00, 1),
(@audio, 'Mikrofoni bežični', 'Set od 2 bežična mikrofona', 'set', 12000.00, 1),
(@light, 'LED rasveta', 'LED rasveta za konferencije', 'komplet', 28000.00, 1),
(@light, 'Bina modul', 'Modularna bina 2x1m', 'kom', 8500.00, 1),
(@stage, 'Zvučnici PA', 'Koncertni zvučnici visoke snage', 'komplet', 52000.00, 1),
(@stage, 'LED rasveta', 'LED video panel', 'kom', 15000.00, 1),
(@stage, 'Mikrofoni bežični', 'Bežični headset mikrofon', 'kom', 6500.00, 1),
(@stage, 'Bina modul', 'Modularna bina premium', 'kom', 9200.00, 1);

INSERT INTO ugovor (
    broj_ugovora, dobavljac_id, dogadjaj_id, datum_potpisivanja, vazi_do,
    vrednost, predmet, uslovi_placanja, dokument_url, status, kreiran_at, updated_at
)
SELECT
    'UG-DEMO-001', @audio, @tech_summit, '2026-05-01', '2026-12-31',
    250000.00, 'Iznajmljivanje audio opreme za Tech Summit', 'Plaćanje u roku od 15 dana',
    'https://example.com/ugovori/ug-demo-001.pdf', 'AKTIVAN', NOW(), NOW()
WHERE @audio IS NOT NULL AND @tech_summit IS NOT NULL;

INSERT INTO ugovor (
    broj_ugovora, dobavljac_id, dogadjaj_id, datum_potpisivanja, vazi_do,
    vrednost, predmet, uslovi_placanja, status, kreiran_at, updated_at
)
SELECT
    'UG-DEMO-002', @light, @fintech, '2026-05-10', '2026-10-31',
    180000.00, 'Rasveta i bina za FinTech Forum', 'Avans 30%, ostatak po isporuci',
    'NACRT', NOW(), NOW()
WHERE @light IS NOT NULL AND @fintech IS NOT NULL;

-- =============================================================================
-- Provera
-- =============================================================================
SELECT 'Dobavljači:' AS info;
SELECT dobavljac_id, naziv, grad, pib, status, rejting
FROM dobavljac
WHERE pib IN ('101234567', '102345678', '103456789');

SELECT 'Cenovnik:' AS info;
SELECT c.cenovnik_id, d.naziv AS dobavljac, c.naziv_resursa, c.cena_jedinicna, c.dostupnost
FROM cenovnik c
JOIN dobavljac d ON c.dobavljac_id = d.dobavljac_id
WHERE d.pib IN ('101234567', '102345678', '103456789');

SELECT 'Ugovori:' AS info;
SELECT broj_ugovora, status, vazi_do, vrednost
FROM ugovor
WHERE broj_ugovora LIKE 'UG-DEMO-%';
