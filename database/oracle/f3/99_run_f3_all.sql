-- Pokretanje celog F3 modula redom.
-- sqlplus ems/ems@//localhost:1522/XEPDB1 @99_run_f3_all.sql

WHENEVER SQLERROR EXIT SQL.SQLCODE
SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE OFF

@00_f3_prerequisite_check.sql
@00_f3_drop.sql
@01_f3_tables.sql
@02_f3_indeksi.sql
@03_f3_types.sql
@04_f3_functions.sql
@05_pkg_budzet_kontrola.pks
@05_pkg_budzet_kontrola.pkb
@06_f3_trg_trosak_cmp.sql
@07_f3_seed_demo.sql
@08_f3_run_demo.sql

PROMPT
PROMPT === F3 PL/SQL modul primenjen ===
PROMPT
