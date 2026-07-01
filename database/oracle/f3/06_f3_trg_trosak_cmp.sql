-- F3 modul: compound trigger na troskovi (izbegava ORA-04091 mutating table).

CREATE OR REPLACE TRIGGER trg_trosak_cmp
FOR INSERT OR UPDATE OR DELETE ON troskovi
COMPOUND TRIGGER

    TYPE t_kljucevi IS TABLE OF obj_stavka_kljuc INDEX BY PLS_INTEGER;
    g_dirnute t_kljucevi;

    AFTER EACH ROW IS
    BEGIN
        IF INSERTING OR UPDATING THEN
            g_dirnute(g_dirnute.COUNT + 1) :=
                obj_stavka_kljuc(:NEW.budzet_id, :NEW.kategorija_id);
        ELSIF DELETING THEN
            g_dirnute(g_dirnute.COUNT + 1) :=
                obj_stavka_kljuc(:OLD.budzet_id, :OLD.kategorija_id);
        END IF;
    END AFTER EACH ROW;

    AFTER STATEMENT IS
    BEGIN
        FOR i IN 1 .. g_dirnute.COUNT LOOP
            pkg_budzet_kontrola.pr_recompute_stavka(
                g_dirnute(i).budzet_id,
                g_dirnute(i).kategorija_id
            );
        END LOOP;
        g_dirnute.DELETE;
    END AFTER STATEMENT;

END trg_trosak_cmp;
/
