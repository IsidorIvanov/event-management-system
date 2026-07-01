-- Pokretanje celog PL/SQL modula redom.
-- sqlplus ems/ems@//localhost:1521/XEPDB1 @07_run_all.sql

WHENEVER SQLERROR EXIT SQL.SQLCODE
SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE OFF

@00_bootstrap_standalone.sql
@01_indeksi.sql
@02_dogadjaj_koordinator.sql
@03_paket_profit_menadzment.pks
@04_paket_profit_menadzment.pkb
@05_trg_analiza_profit.sql
@06_test_simulacija_klijenta.sql

PROMPT
PROMPT === Svi PL/SQL objekti su primenjeni i test blok je izvršen ===
PROMPT
