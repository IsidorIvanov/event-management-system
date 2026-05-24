-- Demo podaci za testiranje aplikacije (događaji, sale, sesije)
-- Pokretanje: mysql -u root -p event_management_db < scripts/seed-demo-data.sql

USE event_management_db;

-- Ukloni prethodne demo podatke (ako postoje)
DELETE s FROM sesija s
INNER JOIN dogadjaj d ON s.dogadjaj_id = d.dogadjaj_id
WHERE d.naziv IN (
    'Tech Summit Beograd 2026',
    'FinTech Forum Sarajevo',
    'Sport Business Day',
    'Digital Days Mostar'
);

DELETE FROM dogadjaj
WHERE naziv IN (
    'Tech Summit Beograd 2026',
    'FinTech Forum Sarajevo',
    'Sport Business Day',
    'Digital Days Mostar'
);

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
(3, 'FinTech Forum Sarajevo', '2026-05-25', '2026-05-26', 350,
 'Forum o digitalnim platnim rešenjima i blockchainu.', 'OBJAVLJEN'),
(2, 'Sport Business Day', '2026-05-27', '2026-05-27', 200,
 'Jednodnevni događaj o sportskom menadžmentu i sponzorstvima.', 'AKTIVAN'),
(6, 'Digital Days Mostar', '2026-05-24', '2026-05-25', 150,
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
(@fintech, 3, 'Auditorijum', 'Startup pitch blok', '2026-05-26', '10:00:00', '12:30:00', 'PANEL', 280, NULL);

-- Sesije — Sport Business Day (lokacija 2 — koristi Sala A ako postoji, inače preskočeno)
-- Dodajemo salu na lokaciji 2 prvo
INSERT INTO sala (lokacija_id, naziv_sale, tip_sale, kapacitet, bazna_cena_po_danu) VALUES
(2, 'Konferencijska sala', 'GLAVNA', 180, 6000.00)
ON DUPLICATE KEY UPDATE kapacitet = VALUES(kapacitet);

INSERT INTO sesija (dogadjaj_id, lokacija_id, naziv_sale, naziv, datum, vreme_pocetka, vreme_zavrsetka, tip, kapacitet, opis) VALUES
(@sport_day, 2, 'Konferencijska sala', 'Sponzorstva u sportu', '2026-05-27', '10:00:00', '12:00:00', 'PANEL', 150, NULL),
(@sport_day, 2, 'Konferencijska sala', 'Marketing sportske brendove', '2026-05-27', '14:00:00', '16:00:00', 'WORKSHOP', 120, NULL);

-- Sesije — Digital Days Mostar (lokacija 6)
INSERT INTO sala (lokacija_id, naziv_sale, tip_sale, kapacitet, bazna_cena_po_danu) VALUES
(6, 'Velika sala', 'GLAVNA', 120, 4000.00)
ON DUPLICATE KEY UPDATE kapacitet = VALUES(kapacitet);

INSERT INTO sesija (dogadjaj_id, lokacija_id, naziv_sale, naziv, datum, vreme_pocetka, vreme_zavrsetka, tip, kapacitet, opis) VALUES
(@digital_days, 6, 'Velika sala', 'Demo dan startapa', '2026-05-24', '11:00:00', '13:00:00', 'PANEL', 100, NULL),
(@digital_days, 6, 'Velika sala', 'UX/UI radionica', '2026-05-24', '15:00:00', '17:00:00', 'WORKSHOP', 60, NULL),
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
