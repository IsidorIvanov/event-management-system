-- Pomoćni SQL za seed-f5-f6-workflow.ps1
-- API nema endpoint za OBJAVLJEN / ZAVRSEN — ove dve komande pokreće skripta.
--
-- Ručno (zameni :dogadjaj_id):
--   UPDATE dogadjaj SET status = 'OBJAVLJEN' WHERE dogadjaj_id = 1;
--   UPDATE dogadjaj SET status = 'ZAVRSEN'  WHERE dogadjaj_id = 1;

-- Parametri koje skripta prosleđuje preko mysql -e:
-- SET @dogadjaj_id = ?;

UPDATE dogadjaj SET status = 'OBJAVLJEN' WHERE dogadjaj_id = @dogadjaj_id;
UPDATE dogadjaj SET status = 'ZAVRSEN'  WHERE dogadjaj_id = @dogadjaj_id;

SELECT dogadjaj_id, naziv, status, datum_pocetka, datum_zavrsetka
FROM dogadjaj
WHERE dogadjaj_id = @dogadjaj_id;
