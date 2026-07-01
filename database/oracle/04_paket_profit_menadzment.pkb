CREATE OR REPLACE PACKAGE BODY paket_profit_menadzment AS

    c_visok_prihod_prag CONSTANT NUMBER := 100000;

    ---------------------------------------------------------------------------
    -- Dinamički SQL + SYS_REFCURSOR (sort po parametru)
    ---------------------------------------------------------------------------
    PROCEDURE izvestaj_analiza_dinamicki (
        p_status       IN  VARCHAR2,
        p_sort_kolona  IN  VARCHAR2,
        p_sort_smer    IN  VARCHAR2,
        p_rezultat     OUT SYS_REFCURSOR
    ) IS
        v_sql   VARCHAR2(4000);
        v_order VARCHAR2(50);
        v_where VARCHAR2(200) := ' WHERE 1=1 ';
    BEGIN
        CASE UPPER(TRIM(p_sort_kolona))
            WHEN 'UKUPAN_PRIHOD' THEN v_order := 'a.ukupan_prihod';
            WHEN 'UKUPAN_TROSAK' THEN v_order := 'a.ukupan_trosak';
            WHEN 'DATUM_ANALIZE' THEN v_order := 'a.datum_analize';
            WHEN 'ANALIZA_ID'    THEN v_order := 'a.analiza_id';
            ELSE
                RAISE_APPLICATION_ERROR(-20002,
                    'Nepoznata kolona za sortiranje: ' || p_sort_kolona);
        END CASE;

        IF UPPER(TRIM(p_sort_smer)) NOT IN ('ASC', 'DESC') THEN
            RAISE_APPLICATION_ERROR(-20002, 'Sort smer mora biti ASC ili DESC');
        END IF;

        IF p_status IS NOT NULL THEN
            v_where := v_where || ' AND a.status = :p_status ';
        END IF;

        v_sql :=
            'SELECT a.analiza_id, a.dogadjaj_id, d.naziv AS dogadjaj_naziv, ' ||
            '       a.ukupan_prihod, a.ukupan_trosak, a.status, a.rezultat_ocene, a.datum_analize ' ||
            'FROM   analize_profitabilnosti a ' ||
            'JOIN   dogadjaj d ON d.dogadjaj_id = a.dogadjaj_id ' ||
            v_where ||
            'ORDER BY ' || v_order || ' ' || UPPER(TRIM(p_sort_smer));

        IF p_status IS NOT NULL THEN
            OPEN p_rezultat FOR v_sql USING p_status;
        ELSE
            OPEN p_rezultat FOR v_sql;
        END IF;
    END izvestaj_analiza_dinamicki;


    ---------------------------------------------------------------------------
    -- Višedimenzionalna kolekcija (niz unutar niza)
    ---------------------------------------------------------------------------
    PROCEDURE ucitaj_matricu_koordinatora (
        p_matrica OUT t_matrica_koordinatora
    ) IS
        CURSOR c_analize IS
            SELECT analiza_id, dogadjaj_id
            FROM   analize_profitabilnosti
            ORDER  BY analiza_id;

        v_idx_koord PLS_INTEGER;
    BEGIN
        p_matrica.DELETE;

        FOR rec IN c_analize LOOP
            p_matrica(rec.analiza_id).DELETE;
            v_idx_koord := 0;

            FOR k IN (
                SELECT zaposleni_id
                FROM   dogadjaj_koordinator
                WHERE  dogadjaj_id = rec.dogadjaj_id
                ORDER  BY zaposleni_id
            ) LOOP
                v_idx_koord := v_idx_koord + 1;
                p_matrica(rec.analiza_id)(v_idx_koord) := k.zaposleni_id;
            END LOOP;
        END LOOP;
    END ucitaj_matricu_koordinatora;


    ---------------------------------------------------------------------------
    -- Eksplicitno upravljanje transakcijom + izuzeci
    ---------------------------------------------------------------------------
    PROCEDURE finalizuj_analizu (
        p_analiza_id IN NUMBER,
        p_napomene   IN VARCHAR2
    ) IS
        v_prihod   analize_profitabilnosti.ukupan_prihod%TYPE;
        v_trosak   analize_profitabilnosti.ukupan_trosak%TYPE;
        v_rezultat analize_profitabilnosti.rezultat_ocene%TYPE;
    BEGIN
        SAVEPOINT sp_pre_finalizacije;

        SELECT ukupan_prihod, ukupan_trosak
        INTO   v_prihod, v_trosak
        FROM   analize_profitabilnosti
        WHERE  analiza_id = p_analiza_id
        FOR UPDATE;

        IF v_prihod > v_trosak THEN
            v_rezultat := 'PROFITABILAN';
        ELSIF v_prihod = v_trosak THEN
            v_rezultat := 'BREAK_EVEN';
        ELSE
            v_rezultat := 'GUBITAK';
        END IF;

        UPDATE analize_profitabilnosti
        SET    status          = 'FINALIZOVANA',
               rezultat_ocene  = v_rezultat,
               finalizovano_at = SYSTIMESTAMP,
               napomene        = NVL(p_napomene, napomene)
        WHERE  analiza_id = p_analiza_id;

        COMMIT;

    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            ROLLBACK TO sp_pre_finalizacije;
            RAISE_APPLICATION_ERROR(-20003, 'Analiza ne postoji: ' || p_analiza_id);
        WHEN OTHERS THEN
            ROLLBACK TO sp_pre_finalizacije;
            RAISE;
    END finalizuj_analizu;


    ---------------------------------------------------------------------------
    -- Iteracija isključivo preko .FIRST i .NEXT
    ---------------------------------------------------------------------------
    PROCEDURE ispisi_matricu (
        p_matrica IN t_matrica_koordinatora
    ) IS
        v_analiza_id PLS_INTEGER;
        v_i          PLS_INTEGER;
    BEGIN
        v_analiza_id := p_matrica.FIRST;

        WHILE v_analiza_id IS NOT NULL LOOP
            DBMS_OUTPUT.PUT_LINE('Analiza ID: ' || v_analiza_id);

            IF p_matrica.EXISTS(v_analiza_id) THEN
                v_i := p_matrica(v_analiza_id).FIRST;

                WHILE v_i IS NOT NULL LOOP
                    DBMS_OUTPUT.PUT_LINE(
                        '  -> Koordinator ID: ' || p_matrica(v_analiza_id)(v_i)
                    );
                    v_i := p_matrica(v_analiza_id).NEXT(v_i);
                END LOOP;
            END IF;

            v_analiza_id := p_matrica.NEXT(v_analiza_id);
        END LOOP;
    END ispisi_matricu;

END paket_profit_menadzment;
/
