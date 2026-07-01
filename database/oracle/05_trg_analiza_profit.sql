-- Row-level trigger sa WHEN klauzulom: zabrana brisanja visoko profitabilnih finalizovanih analiza.

CREATE OR REPLACE TRIGGER trg_analiza_zabrana_brisanja
BEFORE DELETE ON analize_profitabilnosti
FOR EACH ROW
WHEN (
    OLD.status = 'FINALIZOVANA'
    AND OLD.rezultat_ocene = 'PROFITABILAN'
    AND OLD.ukupan_prihod >= 100000
)
BEGIN
    RAISE_APPLICATION_ERROR(
        -20001,
        'Brisanje visoko profitabilne finalizovane analize (ID=' || :OLD.analiza_id || ') nije dozvoljeno.'
    );
END;
/

-- Dodatna validacija INSERT/UPDATE (:NEW / :OLD) bez WHEN (odvojena pravila).
CREATE OR REPLACE TRIGGER trg_analiza_validacija_iznosa
BEFORE INSERT OR UPDATE ON analize_profitabilnosti
FOR EACH ROW
BEGIN
    IF :NEW.ukupan_prihod < 0 OR :NEW.ukupan_trosak < 0 THEN
        RAISE_APPLICATION_ERROR(
            -20005,
            'Ukupan prihod i trošak moraju biti >= 0 (analiza_id=' ||
            NVL(TO_CHAR(:NEW.analiza_id), 'NOVI') || ').'
        );
    END IF;

    IF UPDATING AND :OLD.status = 'FINALIZOVANA' AND :NEW.status = 'DRAFT' THEN
        RAISE_APPLICATION_ERROR(
            -20006,
            'Finalizovana analiza ne može biti vraćena u DRAFT (ID=' || :OLD.analiza_id || ').'
        );
    END IF;
END;
/
