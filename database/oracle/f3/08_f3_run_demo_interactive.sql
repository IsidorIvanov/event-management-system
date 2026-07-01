-- F3 modul: interaktivni demo (SQL Developer / SQL*Plus sa bind promenljivama).
-- Za ručno pokretanje — NE ukljucivati u 99 lanac ako je DEFINE OFF aktivan.

WHENEVER SQLERROR EXIT SQL.SQLCODE
SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE ON

VAR o_rc REFCURSOR;
VAR o_cnt NUMBER;

COLUMN max_bid NEW_VALUE mid NOPRINT
SELECT MAX(budzet_id) AS max_bid FROM budzeti WHERE status = 'ACTIVE';

EXEC pkg_budzet_kontrola.pr_obradi_budzet(&mid, :o_rc, :o_cnt);

PRINT o_cnt
PRINT o_rc
