-- F3 modul: standalone funkcije za NETO troskove, status kontrole i alert nivo.

WHENEVER SQLERROR EXIT SQL.SQLCODE

CREATE OR REPLACE FUNCTION fn_neto_trosak_stavka (
    p_budzet_id     IN NUMBER,
    p_kategorija_id IN NUMBER
) RETURN NUMBER DETERMINISTIC IS
    l_rez NUMBER(15,2);
BEGIN
    SELECT NVL(SUM(iznos - refundirani_iznos), 0)
      INTO l_rez
      FROM troskovi
     WHERE budzet_id = p_budzet_id
       AND kategorija_id = p_kategorija_id;
    RETURN ROUND(l_rez, 2);
END fn_neto_trosak_stavka;
/

CREATE OR REPLACE FUNCTION fn_izracunaj_status_kontrole (
    p_stvarni    IN NUMBER,
    p_planirani  IN NUMBER
) RETURN VARCHAR2 DETERMINISTIC IS
    c_tol_sk CONSTANT NUMBER := 0.02;
    l_ratio  NUMBER;
BEGIN
    IF NVL(p_planirani, 0) = 0 THEN
        IF NVL(p_stvarni, 0) = 0 THEN
            RETURN 'NA_PLANU';
        END IF;
        RETURN 'PREKORACENJE';
    END IF;

    l_ratio := ROUND(p_stvarni / p_planirani, 6);
    IF l_ratio < (1 - c_tol_sk) THEN
        RETURN 'ISPOD_PLANA';
    ELSIF l_ratio <= (1 + c_tol_sk) THEN
        RETURN 'NA_PLANU';
    END IF;
    RETURN 'PREKORACENJE';
END fn_izracunaj_status_kontrole;
/

CREATE OR REPLACE FUNCTION fn_izracunaj_alert_nivo (
    p_stvarni    IN NUMBER,
    p_planirani  IN NUMBER,
    p_prag_u     IN NUMBER,
    p_prag_k     IN NUMBER
) RETURN VARCHAR2 DETERMINISTIC IS
    l_ratio NUMBER;
BEGIN
    IF NVL(p_planirani, 0) = 0 THEN
        IF NVL(p_stvarni, 0) = 0 THEN
            RETURN 'NONE';
        END IF;
        RETURN 'EXCEEDED';
    END IF;

    l_ratio := ROUND(p_stvarni / p_planirani, 6);
    IF l_ratio < NVL(p_prag_u, 0.8) THEN
        RETURN 'NONE';
    ELSIF l_ratio < NVL(p_prag_k, 0.95) THEN
        RETURN 'WARNING';
    ELSIF l_ratio <= 1 THEN
        RETURN 'CRITICAL';
    END IF;
    RETURN 'EXCEEDED';
END fn_izracunaj_alert_nivo;
/

PROMPT === F3 funkcije kreirane ===
