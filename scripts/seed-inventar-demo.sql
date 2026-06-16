-- Demo početne zalihe inventara
-- Pokretanje: mysql -u root -p event_management_db < scripts/seed-inventar-demo.sql

USE event_management_db;

-- Workbench safe update mode: privremeno isključen za čišćenje
SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_demo_oprema;
CREATE TEMPORARY TABLE tmp_demo_oprema (oprema_id BIGINT PRIMARY KEY);

INSERT INTO tmp_demo_oprema (oprema_id)
SELECT oprema_id FROM oprema
WHERE naziv IN (
    'Zvučnici PA', 'LED rasveta', 'Bina modul', 'Mikrofoni bežični'
);

DELETE ik FROM inventar_kretanje ik
INNER JOIN tmp_demo_oprema t ON ik.oprema_id = t.oprema_id;

DELETE d FROM dodela_opreme d
INNER JOIN tmp_demo_oprema t ON d.oprema_id = t.oprema_id;

DELETE o FROM oprema o
INNER JOIN tmp_demo_oprema t ON o.oprema_id = t.oprema_id;

DROP TEMPORARY TABLE IF EXISTS tmp_demo_oprema;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

INSERT INTO oprema (naziv, kategorija, kolicina_ukupno, kolicina_dostupno, kolicina_rezervisano, kolicina_na_dogadjaju, kolicina_u_servisu, status, lokacija_skladista, napomena) VALUES
('Zvučnici PA', 'AUDIO', 4, 4, 0, 0, 0, 'DOSTUPNO', 'Skladište A', 'Demo zaliha'),
('LED rasveta', 'RASVETA', 6, 6, 0, 0, 0, 'DOSTUPNO', 'Skladište A', 'Demo zaliha'),
('Bina modul', 'SCENA', 10, 10, 0, 0, 0, 'DOSTUPNO', 'Skladište B', 'Demo zaliha'),
('Mikrofoni bežični', 'AUDIO', 8, 8, 0, 0, 0, 'DOSTUPNO', 'Skladište A', 'Demo zaliha');

SELECT oprema_id, naziv, kolicina_dostupno, status FROM oprema
WHERE naziv IN ('Zvučnici PA', 'LED rasveta', 'Bina modul', 'Mikrofoni bežični');
