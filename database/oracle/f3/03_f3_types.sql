-- F3 modul: kompleksni tipovi (OBJECT, NESTED TABLE, strong REF CURSOR).

WHENEVER SQLERROR EXIT SQL.SQLCODE

CREATE OR REPLACE TYPE obj_stavka_kljuc AS OBJECT (
    budzet_id     NUMBER,
    kategorija_id NUMBER
);
/

CREATE OR REPLACE TYPE obj_alert_red AS OBJECT (
    kategorija_id    NUMBER,
    kategorija_naziv VARCHAR2(255),
    planirani_iznos  NUMBER(15,2),
    stvarni_iznos    NUMBER(15,2),
    status_kontrole  VARCHAR2(16),
    alert_nivo       VARCHAR2(16),
    ratio            NUMBER(7,4)
);
/

CREATE OR REPLACE TYPE tab_alerti AS TABLE OF obj_alert_red;
/

PROMPT === F3 tipovi kreirani ===
