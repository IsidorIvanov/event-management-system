-- Simulacija poziva sa klijentske strane: REF CURSOR, matrica, trigger test, WHILE petlje.
SET SERVEROUTPUT ON SIZE UNLIMITED

DECLARE
    v_cursor  SYS_REFCURSOR;
    v_matrica paket_profit_menadzment.t_matrica_koordinatora;

    v_analiza_id  analize_profitabilnosti.analiza_id%TYPE;
    v_dogadjaj_id analize_profitabilnosti.dogadjaj_id%TYPE;
    v_dog_naziv   dogadjaj.naziv%TYPE;
    v_prihod      analize_profitabilnosti.ukupan_prihod%TYPE;
    v_trosak      analize_profitabilnosti.ukupan_trosak%TYPE;
    v_status      analize_profitabilnosti.status%TYPE;
    v_rezultat    analize_profitabilnosti.rezultat_ocene%TYPE;
    v_datum       analize_profitabilnosti.datum_analize%TYPE;

    v_draft_id analize_profitabilnosti.analiza_id%TYPE;
BEGIN
    DBMS_OUTPUT.PUT_LINE('========================================');
    DBMS_OUTPUT.PUT_LINE(' PL/SQL TEST — paket_profit_menadzment');
    DBMS_OUTPUT.PUT_LINE('========================================');

    --------------------------------------------------------------------------
    -- 1) Dinamički izveštaj sortiran po prihodu (REF CURSOR)
    --------------------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE(CHR(10) || '=== 1) Dinamički izveštaj (status=FINALIZOVANA, sort=UKUPAN_PRIHOD DESC) ===');

    paket_profit_menadzment.izvestaj_analiza_dinamicki(
        p_status      => 'FINALIZOVANA',
        p_sort_kolona => 'UKUPAN_PRIHOD',
        p_sort_smer   => 'DESC',
        p_rezultat    => v_cursor
    );

    LOOP
        FETCH v_cursor INTO
            v_analiza_id, v_dogadjaj_id, v_dog_naziv,
            v_prihod, v_trosak, v_status, v_rezultat, v_datum;
        EXIT WHEN v_cursor%NOTFOUND;

        DBMS_OUTPUT.PUT_LINE(
            '  #' || v_analiza_id || ' | ' || v_dog_naziv ||
            ' | prihod=' || v_prihod || ' | trošak=' || v_trosak ||
            ' | ' || v_rezultat
        );
    END LOOP;
    CLOSE v_cursor;

    --------------------------------------------------------------------------
    -- 2) Matrica analiza -> koordinatori (.FIRST / .NEXT u paketu)
    --------------------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE(CHR(10) || '=== 2) Matrica analiza -> koordinatori ===');
    paket_profit_menadzment.ucitaj_matricu_koordinatora(v_matrica);
    paket_profit_menadzment.ispisi_matricu(v_matrica);

    --------------------------------------------------------------------------
    -- 3) Finalizacija DRAFT analize (COMMIT u paketu)
    --------------------------------------------------------------------------
    SELECT analiza_id INTO v_draft_id
    FROM   analize_profitabilnosti
    WHERE  status = 'DRAFT'
    FETCH FIRST 1 ROW ONLY;

    DBMS_OUTPUT.PUT_LINE(CHR(10) || '=== 3) Finalizacija DRAFT analize ID=' || v_draft_id || ' ===');
    paket_profit_menadzment.finalizuj_analizu(v_draft_id, 'Finalizovano iz PL/SQL test bloka');

    --------------------------------------------------------------------------
    -- 4) Trigger: pokušaj DELETE visoko profitabilne (očekuje se greška)
    --------------------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE(CHR(10) || '=== 4) Test trigera — DELETE visoko profitabilne ===');
    BEGIN
        DELETE FROM analize_profitabilnosti
        WHERE  analiza_id = (
            SELECT analiza_id
            FROM   analize_profitabilnosti
            WHERE  status = 'FINALIZOVANA'
              AND  rezultat_ocene = 'PROFITABILAN'
              AND  ukupan_prihod >= 100000
            FETCH FIRST 1 ROW ONLY
        );
        DBMS_OUTPUT.PUT_LINE('  GREŠKA: DELETE je prošao a ne bi smeo!');
        ROLLBACK;
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('  OK — trigger blokirao: ' || SQLERRM);
            ROLLBACK;
    END;

    --------------------------------------------------------------------------
    -- 5) Dinamički sort po datumu (druga kolona)
    --------------------------------------------------------------------------
    DBMS_OUTPUT.PUT_LINE(CHR(10) || '=== 5) Dinamički izveštaj (sort=DATUM_ANALIZE ASC) ===');
    paket_profit_menadzment.izvestaj_analiza_dinamicki(
        p_status      => NULL,
        p_sort_kolona => 'DATUM_ANALIZE',
        p_sort_smer   => 'ASC',
        p_rezultat    => v_cursor
    );

    LOOP
        FETCH v_cursor INTO
            v_analiza_id, v_dogadjaj_id, v_dog_naziv,
            v_prihod, v_trosak, v_status, v_rezultat, v_datum;
        EXIT WHEN v_cursor%NOTFOUND;

        DBMS_OUTPUT.PUT_LINE(
            '  #' || v_analiza_id || ' | datum=' || TO_CHAR(v_datum, 'DD.MM.YYYY') ||
            ' | ' || v_status || ' | ' || NVL(v_rezultat, '-')
        );
    END LOOP;
    CLOSE v_cursor;

    DBMS_OUTPUT.PUT_LINE(CHR(10) || '=== TEST ZAVRŠEN USPEŠNO ===');

EXCEPTION
    WHEN OTHERS THEN
        IF v_cursor%ISOPEN THEN
            CLOSE v_cursor;
        END IF;
        ROLLBACK;
        DBMS_OUTPUT.PUT_LINE('TEST NEUSPEŠAN: ' || SQLERRM);
        RAISE;
END;
/
