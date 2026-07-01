-- F3 modul: indeksi za agregaciju troskova i pretragu budzeta po dogadjaju.

WHENEVER SQLERROR EXIT SQL.SQLCODE

CREATE INDEX idx_troskovi_stavka ON troskovi (budzet_id, kategorija_id);
CREATE INDEX idx_budzeti_dogadjaj ON budzeti (dogadjaj_id);

PROMPT === F3 indeksi kreirani ===
