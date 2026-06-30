-- PACKAGE spec: profit management (F5 analiza profitabilnosti)
CREATE OR REPLACE PACKAGE paket_profit_menadzment AS

    -- Pod-niz ID-jeva koordinatora (INDEX BY)
    TYPE t_koordinatori IS TABLE OF NUMBER INDEX BY PLS_INTEGER;

    -- Matrica: ključ = analiza_id, vrednost = pod-niz koordinatora
    TYPE t_matrica_koordinatora IS TABLE OF t_koordinatori INDEX BY PLS_INTEGER;

    -- Dinamički izveštaj sa REF CURSOR
    PROCEDURE izvestaj_analiza_dinamicki (
        p_status       IN  VARCHAR2 DEFAULT NULL,
        p_sort_kolona  IN  VARCHAR2,
        p_sort_smer    IN  VARCHAR2 DEFAULT 'DESC',
        p_rezultat     OUT SYS_REFCURSOR
    );

    -- Učitavanje matrice analiza -> koordinatori u memoriju
    PROCEDURE ucitaj_matricu_koordinatora (
        p_matrica OUT t_matrica_koordinatora
    );

    -- Finalizacija analize sa eksplicitnom transakcijom
    PROCEDURE finalizuj_analizu (
        p_analiza_id IN NUMBER,
        p_napomene   IN VARCHAR2 DEFAULT NULL
    );

    -- Ispis matrice (.FIRST / .NEXT)
    PROCEDURE ispisi_matricu (
        p_matrica IN t_matrica_koordinatora
    );

END paket_profit_menadzment;
/
