# Oracle PL/SQL — modul profit management (F5)

PL/SQL sloj za **analizu profitabilnosti** (`analize_profitabilnosti`), nezavisan od Spring Boot aplikacije koja koristi **MySQL**.

## Šta sadrži

| Fajl | Svrha |
|------|--------|
| `00_bootstrap_standalone.sql` | Minimalna šema + demo podaci (za test bez MySQL) |
| `01_indeksi.sql` | B-Tree indeks `(status, ukupan_prihod)` |
| `02_dogadjaj_koordinator.sql` | Most tabela za matricu koordinatora |
| `03_paket_profit_menadzment.pks` | PACKAGE spec |
| `04_paket_profit_menadzment.pkb` | PACKAGE body (dinamički SQL, kolekcije, transakcije) |
| `05_trg_analiza_profit.sql` | Triggeri sa `WHEN` + validacija `:NEW`/`:OLD` |
| `06_test_simulacija_klijenta.sql` | Anonimni blok — simulacija klijenta |
| `07_run_all.sql` | Sve redom |

## Preduslov: Oracle baza

### Docker (preporučeno)

Iz korena projekta:

```powershell
docker compose up -d oracle-xe
```

Sačekaj ~1–2 min da se baza podigne, zatim:

```powershell
.\scripts\run-oracle-plsql.ps1
```

Konekcija: `ems/ems@//localhost:1522/XEPDB1` (port **1522** ako je 1521 zauzet lokalnim Oracle-om)

### Ručno (SQL Developer / sqlplus)

```bash
sqlplus ems/ems@//localhost:1521/XEPDB1
```

```sql
@database/oracle/07_run_all.sql
```

## Šta test blok proverava

1. **REF CURSOR** — dinamički izveštaj sortiran po `UKUPAN_PRIHOD`
2. **Višedimenzionalna kolekcija** — analiza → lista ID koordinatora (`.FIRST`/`.NEXT`)
3. **Transakcija** — `finalizuj_analizu` sa `COMMIT`/`ROLLBACK`
4. **Trigger + WHEN** — `DELETE` visoko profitabilne analize mora pasti sa `ORA-20001`
5. Drugi dinamički sort po `DATUM_ANALIZE`

## Integracija sa Spring (opciono)

Spring Boot i dalje koristi MySQL (`application.properties`). PL/SQL modul je za **IIS/odbranu** ili budući Oracle profil.

Poziv iz Jave (Oracle JDBC):

```java
jdbcTemplate.execute((Connection con) -> {
    try (CallableStatement cs = con.prepareCall(
            "{ call paket_profit_menadzment.finalizuj_analizu(?, ?) }")) {
        cs.setLong(1, analizaId);
        cs.setString(2, napomene);
        cs.execute();
    }
    return null;
});
```

## Postojeća Oracle šema (bez bootstrap-a)

Ako već imaš tabele iz migracije sa MySQL-a, preskoči `00_bootstrap_standalone.sql` i pokreni samo:

```
01_indeksi.sql → 02 → 03 → 04 → 05 → 06
```

## Poslovna pravila u triggerima

- **DELETE** zabranjen za `FINALIZOVANA` + `PROFITABILAN` + `ukupan_prihod >= 100000`
- **INSERT/UPDATE** — prihod i trošak moraju biti `>= 0`
- **UPDATE** — finalizovana analiza ne može nazad u `DRAFT`
