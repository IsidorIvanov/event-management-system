-- Most tabela: događaj <-> koordinatori (za višedimenzionalne kolekcije u paketu).

BEGIN
    EXECUTE IMMEDIATE '
        CREATE TABLE dogadjaj_koordinator (
            dogadjaj_id   NUMBER(19) NOT NULL,
            zaposleni_id  NUMBER(19) NOT NULL,
            CONSTRAINT pk_dog_koord PRIMARY KEY (dogadjaj_id, zaposleni_id),
            CONSTRAINT fk_dk_dog FOREIGN KEY (dogadjaj_id) REFERENCES dogadjaj(dogadjaj_id),
            CONSTRAINT fk_dk_zap FOREIGN KEY (zaposleni_id) REFERENCES zaposleni(korisnik_id)
        )
    ';
    DBMS_OUTPUT.PUT_LINE('Tabela dogadjaj_koordinator kreirana.');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('Tabela dogadjaj_koordinator već postoji.');
        ELSE
            RAISE;
        END IF;
END;
/

-- Seed: veži koordinatore programa/resursa i kreatora analize za svaki događaj sa analizom.
MERGE INTO dogadjaj_koordinator dk
USING (
    SELECT DISTINCT a.dogadjaj_id, z.korisnik_id AS zaposleni_id
    FROM   analize_profitabilnosti a
    CROSS JOIN zaposleni z
    WHERE  z.uloga IN ('KOORDINATOR_PROGRAMA', 'KOORDINATOR_RESURSA', 'MENADZER_DOGADJAJA')
    UNION
    SELECT DISTINCT a.dogadjaj_id, a.kreirao_id
    FROM   analize_profitabilnosti a
) src
ON (dk.dogadjaj_id = src.dogadjaj_id AND dk.zaposleni_id = src.zaposleni_id)
WHEN NOT MATCHED THEN
    INSERT (dogadjaj_id, zaposleni_id)
    VALUES (src.dogadjaj_id, src.zaposleni_id);

COMMIT;
