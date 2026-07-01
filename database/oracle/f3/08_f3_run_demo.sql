-- F3 modul: demo poziv PR_OBRADI_BUDZET (za 99_run_f3_all.sql lanac).
-- Ne koristi SQL*Plus substitution (&) — kompatibilno sa SET DEFINE OFF.

WHENEVER SQLERROR EXIT SQL.SQLCODE
SET SERVEROUTPUT ON SIZE UNLIMITED

DECLARE
    l_budzet_id NUMBER;
    l_rc        SYS_REFCURSOR;
    l_cnt       NUMBER;
    l_kat_id    NUMBER;
    l_kat_naziv VARCHAR2(200);
    l_plan      NUMBER(15,2);
    l_stvarni   NUMBER(15,2);
    l_status    VARCHAR2(16);
    l_alert     VARCHAR2(16);
    l_ratio     NUMBER(7,4);
BEGIN
    SELECT MAX(budzet_id) INTO l_budzet_id
      FROM budzeti
     WHERE status = 'ACTIVE';

    IF l_budzet_id IS NULL THEN
        RAISE_APPLICATION_ERROR(-20098, 'Nema ACTIVE budzeta za demo.');
    END IF;

    pkg_budzet_kontrola.pr_obradi_budzet(l_budzet_id, l_rc, l_cnt);

    DBMS_OUTPUT.PUT_LINE('=== F3 demo: budzet_id=' || l_budzet_id || ', broj alerta=' || l_cnt || ' ===');

    LOOP
        FETCH l_rc INTO l_kat_id, l_kat_naziv, l_plan, l_stvarni, l_status, l_alert, l_ratio;
        EXIT WHEN l_rc%NOTFOUND;
        DBMS_OUTPUT.PUT_LINE(
            '  ' || l_kat_naziv || ': plan=' || l_plan ||
            ' stvarni=' || l_stvarni || ' alert=' || l_alert ||
            ' ratio=' || NVL(TO_CHAR(l_ratio), 'NULL')
        );
    END LOOP;
    CLOSE l_rc;
END;
/
