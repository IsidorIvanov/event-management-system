-- B-Tree indeks za optimizaciju dinamičkih upita (status + prihod).
-- Idempotentno: ignoriši grešku ako indeks već postoji.

BEGIN
    EXECUTE IMMEDIATE '
        CREATE INDEX idx_analiza_status_prihod
        ON analize_profitabilnosti (status, ukupan_prihod)
    ';
    DBMS_OUTPUT.PUT_LINE('Indeks idx_analiza_status_prihod kreiran.');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('Indeks idx_analiza_status_prihod već postoji.');
        ELSE
            RAISE;
        END IF;
END;
/
