CREATE OR REPLACE PACKAGE BODY pkg_budzet_kontrola AS

    c_tol_sk CONSTANT NUMBER := 0.02;

    TYPE t_stavka_rec IS RECORD (
        budzet_id        stavke_budzeta.budzet_id%TYPE,
        kategorija_id    stavke_budzeta.kategorija_id%TYPE,
        kategorija_naziv budzet_kategorije.naziv%TYPE,
        planirani_iznos  stavke_budzeta.planirani_iznos%TYPE,
        stvarni_iznos    stavke_budzeta.stvarni_iznos%TYPE,
        prag_upozorenja  stavke_budzeta.prag_upozorenja%TYPE,
        prag_kriticnog   stavke_budzeta.prag_kriticnog%TYPE,
        status_kontrole  stavke_budzeta.status_kontrole%TYPE,
        last_alert_level stavke_budzeta.last_alert_level%TYPE
    );

    TYPE t_stavke_tab IS TABLE OF t_stavka_rec INDEX BY PLS_INTEGER;

    CURSOR c_stavke (p_budzet_id IN NUMBER) IS
        SELECT sb.budzet_id,
               sb.kategorija_id,
               bk.naziv AS kategorija_naziv,
               sb.planirani_iznos,
               sb.stvarni_iznos,
               sb.prag_upozorenja,
               sb.prag_kriticnog,
               sb.status_kontrole,
               sb.last_alert_level
          FROM stavke_budzeta sb
          JOIN budzet_kategorije bk ON bk.kategorija_id = sb.kategorija_id
         WHERE sb.budzet_id = p_budzet_id
         ORDER BY bk.naziv;

    ---------------------------------------------------------------------------
    PROCEDURE log_error (
        p_budzet_id IN NUMBER,
        p_poruka    IN VARCHAR2
    ) IS
        PRAGMA AUTONOMOUS_TRANSACTION;
    BEGIN
        INSERT INTO budzet_alert_log (budzet_id, kategorija_id, alert_nivo, poruka)
        VALUES (p_budzet_id, 0, 'ERROR', SUBSTR(p_poruka, 1, 500));
        COMMIT;
    END log_error;

    ---------------------------------------------------------------------------
    PROCEDURE pr_recompute_stavka (
        p_budzet_id     IN NUMBER,
        p_kategorija_id IN NUMBER
    ) IS
        l_planirani stavke_budzeta.planirani_iznos%TYPE;
        l_prag_u    stavke_budzeta.prag_upozorenja%TYPE;
        l_prag_k    stavke_budzeta.prag_kriticnog%TYPE;
        l_stvarni   NUMBER(15,2);
        l_status    VARCHAR2(16);
        l_alert     VARCHAR2(16);
    BEGIN
        SELECT planirani_iznos, prag_upozorenja, prag_kriticnog
          INTO l_planirani, l_prag_u, l_prag_k
          FROM stavke_budzeta
         WHERE budzet_id = p_budzet_id
           AND kategorija_id = p_kategorija_id
           FOR UPDATE;

        l_stvarni := fn_neto_trosak_stavka(p_budzet_id, p_kategorija_id);
        l_status  := fn_izracunaj_status_kontrole(l_stvarni, l_planirani);
        l_alert   := fn_izracunaj_alert_nivo(l_stvarni, l_planirani, l_prag_u, l_prag_k);

        UPDATE stavke_budzeta
           SET stvarni_iznos    = l_stvarni,
               status_kontrole  = l_status,
               last_alert_level = l_alert
         WHERE budzet_id = p_budzet_id
           AND kategorija_id = p_kategorija_id;
    END pr_recompute_stavka;

    ---------------------------------------------------------------------------
    PROCEDURE pr_obradi_budzet (
        p_budzet_id    IN  NUMBER,
        p_upozorenja   OUT SYS_REFCURSOR,
        p_broj_alerta  OUT NUMBER
    ) IS
        l_status_budzeta budzeti.status%TYPE;
        l_stavke         t_stavke_tab;
        l_alerti         tab_alerti := tab_alerti();
        l_i              PLS_INTEGER;
        l_stvarni        NUMBER(15,2);
        l_status         VARCHAR2(16);
        l_alert          VARCHAR2(16);
        l_ratio          NUMBER(7,4);
        l_broj           NUMBER := 0;
    BEGIN
        SELECT status
          INTO l_status_budzeta
          FROM budzeti
         WHERE budzet_id = p_budzet_id;

        IF l_status_budzeta <> 'ACTIVE' THEN
            RAISE ex_budzet_nije_aktivan;
        END IF;

        l_i := 0;
        FOR r IN c_stavke(p_budzet_id) LOOP
            l_i := l_i + 1;
            l_stavke(l_i).budzet_id        := r.budzet_id;
            l_stavke(l_i).kategorija_id    := r.kategorija_id;
            l_stavke(l_i).kategorija_naziv := r.kategorija_naziv;
            l_stavke(l_i).planirani_iznos  := r.planirani_iznos;
            l_stavke(l_i).prag_upozorenja  := r.prag_upozorenja;
            l_stavke(l_i).prag_kriticnog   := r.prag_kriticnog;
            l_stavke(l_i).last_alert_level := r.last_alert_level;

            l_stvarni := fn_neto_trosak_stavka(r.budzet_id, r.kategorija_id);
            l_status  := fn_izracunaj_status_kontrole(l_stvarni, r.planirani_iznos);
            l_alert   := fn_izracunaj_alert_nivo(
                l_stvarni, r.planirani_iznos, r.prag_upozorenja, r.prag_kriticnog
            );

            l_stavke(l_i).stvarni_iznos    := l_stvarni;
            l_stavke(l_i).status_kontrole  := l_status;
            l_stavke(l_i).last_alert_level := l_alert;

            IF r.planirani_iznos > 0 THEN
                l_ratio := ROUND(l_stvarni / r.planirani_iznos, 4);
            ELSE
                l_ratio := NULL;
            END IF;

            l_alerti.EXTEND;
            l_alerti(l_alerti.COUNT) := obj_alert_red(
                r.kategorija_id,
                r.kategorija_naziv,
                r.planirani_iznos,
                l_stvarni,
                l_status,
                l_alert,
                l_ratio
            );
        END LOOP;

        IF l_i > 0 THEN
            FORALL j IN 1 .. l_i
                UPDATE stavke_budzeta
                   SET stvarni_iznos    = l_stavke(j).stvarni_iznos,
                       status_kontrole  = l_stavke(j).status_kontrole,
                       last_alert_level = l_stavke(j).last_alert_level
                 WHERE budzet_id = l_stavke(j).budzet_id
                   AND kategorija_id = l_stavke(j).kategorija_id;
        END IF;

        l_i := 1;
        WHILE l_i <= l_alerti.COUNT LOOP
            IF l_alerti(l_i).alert_nivo IN ('WARNING', 'CRITICAL', 'EXCEEDED') THEN
                INSERT INTO budzet_alert_log (
                    budzet_id, kategorija_id, kategorija_naziv,
                    alert_nivo, ratio, poruka
                ) VALUES (
                    p_budzet_id,
                    l_alerti(l_i).kategorija_id,
                    l_alerti(l_i).kategorija_naziv,
                    l_alerti(l_i).alert_nivo,
                    l_alerti(l_i).ratio,
                    'F3 alert: ' || l_alerti(l_i).kategorija_naziv ||
                    ' nivo ' || l_alerti(l_i).alert_nivo
                );
                l_broj := l_broj + 1;
            END IF;
            l_i := l_i + 1;
        END LOOP;

        p_broj_alerta := l_broj;

        OPEN p_upozorenja FOR
            SELECT sb.kategorija_id,
                   bk.naziv AS kategorija_naziv,
                   sb.planirani_iznos,
                   sb.stvarni_iznos,
                   sb.status_kontrole,
                   sb.last_alert_level AS alert_nivo,
                   CASE
                       WHEN sb.planirani_iznos > 0
                       THEN ROUND(sb.stvarni_iznos / sb.planirani_iznos, 4)
                       ELSE NULL
                   END AS ratio
              FROM stavke_budzeta sb
              JOIN budzet_kategorije bk ON bk.kategorija_id = sb.kategorija_id
             WHERE sb.budzet_id = p_budzet_id
               AND sb.last_alert_level IN ('WARNING', 'CRITICAL', 'EXCEEDED')
             ORDER BY ratio DESC NULLS LAST;

    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20002, 'Budzet ne postoji: id=' || p_budzet_id);
        WHEN ex_budzet_nije_aktivan THEN
            RAISE_APPLICATION_ERROR(-20001, 'Budzet mora biti ACTIVE (id=' || p_budzet_id || ')');
        WHEN OTHERS THEN
            log_error(p_budzet_id, SQLERRM);
            RAISE;
    END pr_obradi_budzet;

END pkg_budzet_kontrola;
/
