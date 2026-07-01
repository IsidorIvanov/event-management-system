-- F3 modul: provera preduslova pre kreiranja objekata.
-- Ne koristi SELECT FROM dogadjaj (ORA-00942 bez jasne poruke).

WHENEVER SQLERROR EXIT SQL.SQLCODE
SET SERVEROUTPUT ON

DECLARE
  l_tab_cnt NUMBER;
  l_row_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO l_tab_cnt
  FROM user_tables
  WHERE table_name = 'DOGADJAJ';

  IF l_tab_cnt = 0 THEN
    RAISE_APPLICATION_ERROR(-20099,
      'Preduslov nije ispunjen: tabela DOGADJAJ ne postoji. ' ||
      'Pokrenite osnovni Oracle bootstrap pre F3 modula.');
  END IF;

  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM dogadjaj' INTO l_row_cnt;
  IF l_row_cnt = 0 THEN
    RAISE_APPLICATION_ERROR(-20098,
      'Preduslov nije ispunjen: tabela DOGADJAJ je prazna. ' ||
      'Dodajte bar jedan dogadjaj pre F3 seed-a.');
  END IF;

  DBMS_OUTPUT.PUT_LINE('F3 prerequisite OK: DOGADJAJ postoji (' || l_row_cnt || ' redova).');
END;
/
