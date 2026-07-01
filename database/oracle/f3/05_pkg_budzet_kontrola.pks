-- PACKAGE spec: F3 budzetska kontrola i upozorenja

CREATE OR REPLACE PACKAGE pkg_budzet_kontrola AS

    -- Strong REF CURSOR (%ROWTYPE je validan u package spec, ne kao standalone SQL TYPE)
    TYPE typ_ref_stavke IS REF CURSOR RETURN stavke_budzeta%ROWTYPE;

    ex_budzet_nije_aktivan EXCEPTION;
    PRAGMA EXCEPTION_INIT(ex_budzet_nije_aktivan, -20001);

    PROCEDURE pr_obradi_budzet (
        p_budzet_id    IN  NUMBER,
        p_upozorenja   OUT SYS_REFCURSOR,
        p_broj_alerta  OUT NUMBER
    );

    PROCEDURE pr_recompute_stavka (
        p_budzet_id     IN NUMBER,
        p_kategorija_id IN NUMBER
    );

END pkg_budzet_kontrola;
/
