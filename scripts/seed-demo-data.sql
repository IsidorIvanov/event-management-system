-- Demo podaci za testiranje aplikacije (događaji, sale, sesije)
-- Pokretanje: mysql -u root -p event_management_db < scripts/seed-demo-data.sql

USE event_management_db;

-- Workbench safe update mode: privremeno isključen za čišćenje demo podataka
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- Ukloni prethodne demo podatke (FK-safe redosled)
DROP TEMPORARY TABLE IF EXISTS tmp_demo_dogadjaj;
CREATE TEMPORARY TABLE tmp_demo_dogadjaj (dogadjaj_id BIGINT PRIMARY KEY);

INSERT INTO tmp_demo_dogadjaj (dogadjaj_id)
SELECT dogadjaj_id FROM dogadjaj
WHERE naziv IN (
    'Tech Summit Beograd 2026',
    'FinTech Forum Sarajevo',
    'Sport Business Day',
    'Digital Days Mostar'
);

DROP TEMPORARY TABLE IF EXISTS tmp_demo_sesija;
CREATE TEMPORARY TABLE tmp_demo_sesija (sesija_id BIGINT PRIMARY KEY);

INSERT INTO tmp_demo_sesija (sesija_id)
SELECT s.sesija_id FROM sesija s
INNER JOIN tmp_demo_dogadjaj t ON s.dogadjaj_id = t.dogadjaj_id;

-- Inventar / dodele opreme
DELETE ik FROM inventar_kretanje ik
INNER JOIN dodela_opreme d ON ik.dodela_id = d.dodela_id
INNER JOIN tmp_demo_dogadjaj t ON d.dogadjaj_id = t.dogadjaj_id;

DELETE d FROM dodela_opreme d
INNER JOIN tmp_demo_dogadjaj t ON d.dogadjaj_id = t.dogadjaj_id;

-- Sesije i povezani zapisi
DELETE us FROM ucesnik_sesija us
INNER JOIN tmp_demo_sesija ts ON us.sesija_id = ts.sesija_id;

DELETE sg FROM sesija_govornik sg
INNER JOIN tmp_demo_sesija ts ON sg.sesija_id = ts.sesija_id;

DELETE s FROM sesija s
INNER JOIN tmp_demo_dogadjaj t ON s.dogadjaj_id = t.dogadjaj_id;

-- Karte i registracije
DELETE r FROM registracija r
INNER JOIN tmp_demo_dogadjaj t ON r.dogadjaj_id = t.dogadjaj_id;

DELETE tk FROM tip_karte tk
INNER JOIN tmp_demo_dogadjaj t ON tk.dogadjaj_id = t.dogadjaj_id;

-- Fakture i plaćanja
DROP TEMPORARY TABLE IF EXISTS tmp_demo_faktura;
CREATE TEMPORARY TABLE tmp_demo_faktura (faktura_id BIGINT PRIMARY KEY);

INSERT INTO tmp_demo_faktura (faktura_id)
SELECT f.faktura_id FROM faktura f
INNER JOIN tmp_demo_dogadjaj t ON f.dogadjaj_id = t.dogadjaj_id;

DELETE rf FROM refundacija rf
INNER JOIN placanje p ON rf.placanje_id = p.placanje_id
INNER JOIN tmp_demo_faktura tf ON p.faktura_id = tf.faktura_id;

DELETE p FROM placanje p
INNER JOIN tmp_demo_faktura tf ON p.faktura_id = tf.faktura_id;

UPDATE trosak tr
INNER JOIN tmp_demo_faktura tf ON tr.faktura_id = tf.faktura_id
SET tr.faktura_id = NULL, tr.stavka_fakture_id = NULL;

DELETE sf FROM stavka_fakture sf
INNER JOIN tmp_demo_faktura tf ON sf.faktura_id = tf.faktura_id;

DELETE f FROM faktura f
INNER JOIN tmp_demo_faktura tf ON f.faktura_id = tf.faktura_id;

-- Nabavke i ugovori
DROP TEMPORARY TABLE IF EXISTS tmp_demo_nabavka;
CREATE TEMPORARY TABLE tmp_demo_nabavka (nabavka_id BIGINT PRIMARY KEY);

INSERT INTO tmp_demo_nabavka (nabavka_id)
SELECT n.nabavka_id FROM nabavka n
INNER JOIN tmp_demo_dogadjaj t ON n.dogadjaj_id = t.dogadjaj_id;

DELETE sp FROM stavka_porudzbenice sp
INNER JOIN porudzbenica po ON sp.porudzbenica_id = po.porudzbenica_id
INNER JOIN tmp_demo_nabavka tn ON po.nabavka_id = tn.nabavka_id;

DELETE po FROM porudzbenica po
INNER JOIN tmp_demo_nabavka tn ON po.nabavka_id = tn.nabavka_id;

DELETE sn FROM stavka_nabavke sn
INNER JOIN tmp_demo_nabavka tn ON sn.nabavka_id = tn.nabavka_id;

UPDATE ugovor u
INNER JOIN tmp_demo_nabavka tn ON u.nabavka_id = tn.nabavka_id
SET u.nabavka_id = NULL;

DELETE u FROM ugovor u
INNER JOIN tmp_demo_dogadjaj t ON u.dogadjaj_id = t.dogadjaj_id;

DELETE n FROM nabavka n
INNER JOIN tmp_demo_nabavka tn ON n.nabavka_id = tn.nabavka_id;

-- Budžet i troškovi
DROP TEMPORARY TABLE IF EXISTS tmp_demo_budzet;
CREATE TEMPORARY TABLE tmp_demo_budzet (budzet_id BIGINT PRIMARY KEY);

INSERT INTO tmp_demo_budzet (budzet_id)
SELECT b.budzet_id FROM budzet b
INNER JOIN tmp_demo_dogadjaj t ON b.dogadjaj_id = t.dogadjaj_id;

DELETE tr FROM trosak tr
INNER JOIN tmp_demo_budzet tb ON tr.budzet_id = tb.budzet_id;

DELETE tr FROM trosak tr
INNER JOIN tmp_demo_dogadjaj t ON tr.dogadjaj_id = t.dogadjaj_id;

DELETE sb FROM stavka_budzeta sb
INNER JOIN tmp_demo_budzet tb ON sb.budzet_id = tb.budzet_id;

DELETE b FROM budzet b
INNER JOIN tmp_demo_budzet tb ON b.budzet_id = tb.budzet_id;

-- Analize, prognoze, preporuke, notifikacije
DELETE a FROM analize_profitabilnosti a
INNER JOIN tmp_demo_dogadjaj t ON a.dogadjaj_id = t.dogadjaj_id;

DELETE sc FROM scenariji_prognoze sc
INNER JOIN tmp_demo_dogadjaj t ON sc.dogadjaj_id = t.dogadjaj_id;

DELETE pr FROM preporuka pr
INNER JOIN tmp_demo_dogadjaj t ON pr.dogadjaj_id = t.dogadjaj_id;

DELETE no FROM notifikacija no
INNER JOIN tmp_demo_dogadjaj t ON no.dogadjaj_id = t.dogadjaj_id;

DELETE dt FROM dogadjaj_tag dt
INNER JOIN tmp_demo_dogadjaj t ON dt.dogadjaj_id = t.dogadjaj_id;

DELETE d FROM dogadjaj d
INNER JOIN tmp_demo_dogadjaj t ON d.dogadjaj_id = t.dogadjaj_id;

DROP TEMPORARY TABLE IF EXISTS tmp_demo_faktura;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_nabavka;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_budzet;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_sesija;
DROP TEMPORARY TABLE IF EXISTS tmp_demo_dogadjaj;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

-- Dodatne sale (za kalendar dostupnosti)
INSERT INTO sala (lokacija_id, naziv_sale, tip_sale, kapacitet, bazna_cena_po_danu) VALUES
(1, 'Sala B', 'WORKSHOP', 80, 3500.00),
(1, 'Glavna dvorana', 'GLAVNA', 500, 12000.00),
(3, 'Auditorijum', 'GLAVNA', 300, 8000.00),
(3, 'Sala 2', 'WORKSHOP', 50, 2500.00),
(11, 'Arena', 'GLAVNA', 1000, 15000.00)
ON DUPLICATE KEY UPDATE
    tip_sale = VALUES(tip_sale),
    kapacitet = VALUES(kapacitet),
    bazna_cena_po_danu = VALUES(bazna_cena_po_danu);

-- Događaji
INSERT INTO dogadjaj (lokacija_id, naziv, datum_pocetka, datum_zavrsetka, maks_kapacitet, opis, status) VALUES
(1, 'Tech Summit Beograd 2026', '2026-05-26', '2026-05-28', 800,
 'Godišnja IT konferencija sa keynote predavanjima i radionicama.', 'AKTIVAN'),
(3, 'FinTech Forum Sarajevo', '2026-05-25', '2026-05-26', 500,
 'Forum o digitalnim platnim rešenjima i blockchainu.', 'OBJAVLJEN'),
(2, 'Sport Business Day', '2026-05-27', '2026-05-27', 500,
 'Jednodnevni događaj o sportskom menadžmentu i sponzorstvima.', 'AKTIVAN'),
(6, 'Digital Days Mostar', '2026-05-24', '2026-05-25', 250,
 'Regionalni sajam digitalnih kompanija i startapa.', 'DRAFT');

SET @tech_summit = (SELECT dogadjaj_id FROM dogadjaj WHERE naziv = 'Tech Summit Beograd 2026' LIMIT 1);
SET @fintech = (SELECT dogadjaj_id FROM dogadjaj WHERE naziv = 'FinTech Forum Sarajevo' LIMIT 1);
SET @sport_day = (SELECT dogadjaj_id FROM dogadjaj WHERE naziv = 'Sport Business Day' LIMIT 1);
SET @digital_days = (SELECT dogadjaj_id FROM dogadjaj WHERE naziv = 'Digital Days Mostar' LIMIT 1);

-- Sesije — Tech Summit (lokacija 1, Konferencijski centar)
INSERT INTO sesija (dogadjaj_id, lokacija_id, naziv_sale, naziv, datum, vreme_pocetka, vreme_zavrsetka, tip, kapacitet, opis) VALUES
(@tech_summit, 1, 'Sala A', 'Otvorenje i uvodni keynote', '2026-05-26', '10:00:00', '12:00:00', 'KEYNOTE', 120, NULL),
(@tech_summit, 1, 'Sala A', 'Panel: AI u industriji', '2026-05-26', '14:00:00', '16:00:00', 'PANEL', 100, NULL),
(@tech_summit, 1, 'Sala B', 'Workshop: React napredno', '2026-05-26', '10:00:00', '13:00:00', 'WORKSHOP', 40, NULL),
(@tech_summit, 1, 'Glavna dvorana', 'Večernje networking okupljanje', '2026-05-26', '18:00:00', '20:00:00', 'NETWORKING', 500, NULL),
(@tech_summit, 1, 'Sala A', 'Cloud arhitektura', '2026-05-27', '09:00:00', '11:00:00', 'WORKSHOP', 80, NULL),
(@tech_summit, 1, 'Sala B', 'DevOps prakse', '2026-05-27', '11:00:00', '13:00:00', 'WORKSHOP', 50, NULL),
(@tech_summit, 1, 'Glavna dvorana', 'Zaključni keynote', '2026-05-28', '10:00:00', '12:00:00', 'KEYNOTE', 500, NULL);

-- Sesije — FinTech Forum (lokacija 3, BBI Centar)
INSERT INTO sesija (dogadjaj_id, lokacija_id, naziv_sale, naziv, datum, vreme_pocetka, vreme_zavrsetka, tip, kapacitet, opis) VALUES
(@fintech, 3, 'Auditorijum', 'Budućnost digitalnih banaka', '2026-05-25', '09:00:00', '11:00:00', 'KEYNOTE', 250, NULL),
(@fintech, 3, 'Sala 2', 'Regulativa i compliance', '2026-05-25', '13:00:00', '15:00:00', 'PANEL', 45, NULL),
(@fintech, 3, 'Auditorijum', 'Startup pitch blok', '2026-05-26', '10:00:00', '12:30:00', 'PANEL', 280, NULL),
(@fintech, 3, 'Sala 2', 'Workshop: blockchain u praksi', '2026-05-25', '16:00:00', '18:00:00', 'WORKSHOP', 40, NULL),
(@fintech, 3, 'Sala 2', 'Workshop: PSD2 i open banking', '2026-05-26', '14:00:00', '16:00:00', 'WORKSHOP', 45, NULL);

-- Sesije — Sport Business Day (lokacija 2 — koristi Sala A ako postoji, inače preskočeno)
-- Dodajemo salu na lokaciji 2 prvo
INSERT INTO sala (lokacija_id, naziv_sale, tip_sale, kapacitet, bazna_cena_po_danu) VALUES
(2, 'Konferencijska sala', 'GLAVNA', 180, 6000.00)
ON DUPLICATE KEY UPDATE kapacitet = VALUES(kapacitet);

INSERT INTO sesija (dogadjaj_id, lokacija_id, naziv_sale, naziv, datum, vreme_pocetka, vreme_zavrsetka, tip, kapacitet, opis) VALUES
(@sport_day, 2, 'Konferencijska sala', 'Uvodni keynote: budućnost sportskog biznisa', '2026-05-27', '09:00:00', '10:00:00', 'KEYNOTE', 180, NULL),
(@sport_day, 2, 'Konferencijska sala', 'Sponzorstva u sportu', '2026-05-27', '10:00:00', '12:00:00', 'PANEL', 150, NULL),
(@sport_day, 2, 'Konferencijska sala', 'Panel: medijski prava i prenosi', '2026-05-27', '12:30:00', '13:30:00', 'PANEL', 140, NULL),
(@sport_day, 2, 'Konferencijska sala', 'Marketing sportske brendove', '2026-05-27', '14:00:00', '16:00:00', 'WORKSHOP', 120, NULL);

-- Sesije — Digital Days Mostar (lokacija 6)
INSERT INTO sala (lokacija_id, naziv_sale, tip_sale, kapacitet, bazna_cena_po_danu) VALUES
(6, 'Velika sala', 'GLAVNA', 120, 4000.00)
ON DUPLICATE KEY UPDATE kapacitet = VALUES(kapacitet);

INSERT INTO sesija (dogadjaj_id, lokacija_id, naziv_sale, naziv, datum, vreme_pocetka, vreme_zavrsetka, tip, kapacitet, opis) VALUES
(@digital_days, 6, 'Velika sala', 'Otvorenje i keynote', '2026-05-24', '10:00:00', '11:00:00', 'KEYNOTE', 110, NULL),
(@digital_days, 6, 'Velika sala', 'Demo dan startapa', '2026-05-24', '11:00:00', '13:00:00', 'PANEL', 100, NULL),
(@digital_days, 6, 'Velika sala', 'UX/UI radionica', '2026-05-24', '15:00:00', '17:00:00', 'WORKSHOP', 60, NULL),
(@digital_days, 6, 'Velika sala', 'Workshop: digitalni marketing', '2026-05-25', '10:00:00', '12:00:00', 'WORKSHOP', 55, NULL),
(@digital_days, 6, 'Velika sala', 'Networking posle podne', '2026-05-25', '16:00:00', '18:00:00', 'NETWORKING', 90, NULL);

SELECT 'Događaji:' AS info;
SELECT dogadjaj_id, naziv, lokacija_id, datum_pocetka, datum_zavrsetka, status FROM dogadjaj
WHERE naziv IN (
    'Tech Summit Beograd 2026',
    'FinTech Forum Sarajevo',
    'Sport Business Day',
    'Digital Days Mostar'
);

SELECT 'Sesije (ukupno):' AS info, COUNT(*) AS broj FROM sesija;
