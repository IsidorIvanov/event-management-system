-- F3 modul: demo podaci — 4 alert nivoa (NONE / WARNING / CRITICAL / EXCEEDED).

WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF

DECLARE
    l_dogadjaj_id     NUMBER(19);
    l_budzet_id       NUMBER(19);
    l_kat_marketing   NUMBER(19);
    l_kat_ketering    NUMBER(19);
    l_kat_oprema      NUMBER(19);
    l_kat_tehnika     NUMBER(19);
BEGIN
    SELECT dogadjaj_id INTO l_dogadjaj_id
      FROM (
          SELECT dogadjaj_id
            FROM dogadjaj
           ORDER BY dogadjaj_id
      )
     WHERE ROWNUM = 1;

    INSERT INTO budzet_kategorije (naziv) VALUES ('Marketing')
        RETURNING kategorija_id INTO l_kat_marketing;
    INSERT INTO budzet_kategorije (naziv) VALUES ('Ketering')
        RETURNING kategorija_id INTO l_kat_ketering;
    INSERT INTO budzet_kategorije (naziv) VALUES ('Oprema')
        RETURNING kategorija_id INTO l_kat_oprema;
    INSERT INTO budzet_kategorije (naziv) VALUES ('Tehnika')
        RETURNING kategorija_id INTO l_kat_tehnika;

    INSERT INTO budzeti (
        dogadjaj_id, naziv_budzeta, planirani_iznos, odobreni_iznos, status
    ) VALUES (
        l_dogadjaj_id,
        'F3 Demo Budzet - PL/SQL Demo Konferencija',
        280000, 280000, 'ACTIVE'
    ) RETURNING budzet_id INTO l_budzet_id;

    INSERT INTO stavke_budzeta (budzet_id, kategorija_id, planirani_iznos)
    VALUES (l_budzet_id, l_kat_marketing, 30000);

    INSERT INTO stavke_budzeta (budzet_id, kategorija_id, planirani_iznos)
    VALUES (l_budzet_id, l_kat_ketering, 100000);

    INSERT INTO stavke_budzeta (budzet_id, kategorija_id, planirani_iznos)
    VALUES (l_budzet_id, l_kat_oprema, 100000);

    INSERT INTO stavke_budzeta (budzet_id, kategorija_id, planirani_iznos)
    VALUES (l_budzet_id, l_kat_tehnika, 50000);

    -- Marketing: 10.000 -> NONE
    INSERT INTO troskovi (budzet_id, kategorija_id, opis, iznos, tip_troska)
    VALUES (l_budzet_id, l_kat_marketing, 'Demo marketing', 10000, 'RUCNI');

    -- Ketering: 90.000 bruto, 5.000 refund -> 85.000 NETO -> WARNING
    INSERT INTO troskovi (budzet_id, kategorija_id, opis, iznos, refundirani_iznos, tip_troska)
    VALUES (l_budzet_id, l_kat_ketering, 'Demo ketering sa refundacijom', 90000, 5000, 'RUCNI');

    -- Oprema: 97.000 -> CRITICAL
    INSERT INTO troskovi (budzet_id, kategorija_id, opis, iznos, tip_troska)
    VALUES (l_budzet_id, l_kat_oprema, 'Demo oprema', 97000, 'RUCNI');

    -- Tehnika: 52.000 -> EXCEEDED
    INSERT INTO troskovi (budzet_id, kategorija_id, opis, iznos, tip_troska)
    VALUES (l_budzet_id, l_kat_tehnika, 'Demo tehnika', 52000, 'RUCNI');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('F3 seed OK: budzet_id=' || l_budzet_id);
END;
/
