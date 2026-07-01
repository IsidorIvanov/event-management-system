# Oracle PL/SQL — F3 budžetska kontrola i upozorenja

PL/SQL sloj za **F3** (kontrola budžeta, `stvarni_iznos`, upozorenja WARNING/CRITICAL/EXCEEDED).
Paralelan je postojećoj **Java/MySQL** implementaciji (`BudzetService`) — ne zamenjuje je.

**Paket:** `PKG_BUDZET_KONTROLA`  
**Folder:** `database/oracle/f3/` (samo ovaj modul je deo ovog README-a)

---

## Odnos prema drugim implementacijama

| Implementacija | Gde | Uloga |
|----------------|-----|-------|
| **F3 Java/MySQL** | `BudzetService`, `/api/budzet/**` | Primarna aplikacija |
| **F3 Oracle PL/SQL (ovaj modul)** | `database/oracle/f3/`, `/api/budzet/oracle/**` | JDBC sidecar za IIS/odbranu |
| **MySQL SBP (registracije)** | `RegistracijaProcedureDao` | Drugi proceduralni sloj — MySQL, ne Oracle |

Oracle podaci su **odvojeni** od MySQL — nema sinhronizacije.

---

## Preduslov

1. Oracle XE kontejner: `docker compose up -d oracle-xe`
2. U šemi mora postojati tabela **`dogadjaj`** sa bar jednim redom (kreira je osnovni Oracle bootstrap van ovog foldera).
3. `00_f3_prerequisite_check.sql` proverava preko `user_tables` (jasna poruka ako nedostaje).

**Re-run:** F3 modul briše samo svoje objekte — **nikad** `dogadjaj`.

---

## Pokretanje

```powershell
# Posle osnovnog Oracle bootstrap-a u šemi:
.\scripts\run-oracle-plsql.ps1 -Module F3
```

Ručno (SQL*Plus u kontejneru, radni folder `/tmp/f3`):

```sql
@99_run_f3_all.sql
```

Konekcija sa hosta: `ems/ems@//localhost:1522/XEPDB1`

---

## Fajlovi

| Fajl | Svrha |
|------|--------|
| `00_f3_prerequisite_check.sql` | Provera `DOGADJAJ` (data dictionary) |
| `00_f3_drop.sql` | Idempotentni DROP samo F3 objekata |
| `01_f3_tables.sql` | Tabele budžeta i troškova |
| `02_f3_indeksi.sql` | `idx_troskovi_stavka`, `idx_budzeti_dogadjaj` |
| `03_f3_types.sql` | OBJECT, NESTED TABLE, strong REF CURSOR |
| `04_f3_functions.sql` | NETO trošak, status, alert nivo |
| `05_pkg_budzet_kontrola.pks/.pkb` | Paket + `PR_OBRADI_BUDZET`, `FOR UPDATE` |
| `06_f3_trg_trosak_cmp.sql` | Compound trigger (mutating table) |
| `07_f3_seed_demo.sql` | 4 alert nivoa demo |
| `08_f3_run_demo.sql` | Demo poziv procedure (PL/SQL blok, za `99` lanac) |
| `08_f3_run_demo_interactive.sql` | Interaktivni demo (`VAR`/`PRINT`, SQL Developer) |
| `99_run_f3_all.sql` | Sve redom |

---

## Mapa PL/SQL konstrukcija

| Konstrukcija | Gde |
|--------------|-----|
| Eksplicitni kursor | `c_stavke` u `.pkb` |
| Strong REF CURSOR tip | `typ_ref_stavke` u `05_pkg_budzet_kontrola.pks` (package spec) |
| SYS_REFCURSOR OUT | `PR_OBRADI_BUDZET` |
| FOR petlja | `pr_obradi_budzet` |
| WHILE petlja | iteracija `tab_alerti` pre log upisa |
| BULK COLLECT / FORALL | batch UPDATE stavki u `.pkb` |
| Funkcije | `04_f3_functions.sql` |
| Compound trigger | `06_f3_trg_trosak_cmp.sql` |
| SELECT FOR UPDATE | `pr_recompute_stavka` |
| Exception | `-20001`..`-20099` |
| Indeksi | `02_f3_indeksi.sql` |

---

## Dizajn — zašto ovako

### Kolekcija `tab_alerti` + REF CURSOR na kraju (#6)

`tab_alerti` (NESTED TABLE) drži **in-memory** rezultat evaluacije tokom jednog prolaza kroz stavke — WHILE petlja upisuje samo redove sa WARNING/CRITICAL/EXCEEDED u `budzet_alert_log`. REF CURSOR na izlazu čita iz **tabele** (`stavke_budzeta`) posle FORALL UPDATE-a, da klijent dobije konzistentan snapshot posle upisa. Razdvajanje: računanje u memoriji → batch UPDATE → audit log → izveštaj kursorom.

### Trigger vs. `pr_obradi_budzet` (#7)

- **Compound trigger** (`trg_trosak_cmp`) → `pr_recompute_stavka`: inkrementalno održavanje **jedne stavke** posle INSERT/UPDATE/DELETE troška (bez alert loga).
- **`pr_obradi_budzet`**: puna rekalkulacija **celog budžeta**, upis alert loga i REF CURSOR izveštaj.

Oba računaju `stvarni_iznos` — idempotentno je; trigger pokriva svakodnevne promene, procedura služi za kontrolu i odbranu.

### `SELECT FOR UPDATE` (#8)

`FOR UPDATE` je u **`pr_recompute_stavka`** (poziva ga trigger pri promeni troška) — zaključava stavku dok se NETO trošak ne prepiše, što sprečava lost update pri konkurentnim INSERT-ima troškova.

`pr_obradi_budzet` nema `FOR UPDATE` — read-mostly izveštajna procedura; za odbranu naglasiti da je lock na putu troška→trigger, ne na batch izveštaju.

### Error log u `WHEN OTHERS`

`log_error` koristi `PRAGMA AUTONOMOUS_TRANSACTION` — greška se upisuje u `budzet_alert_log` i preživljava rollback glavne transakcije.

---

## Spring Boot JDBC (opciono)

```properties
app.oracle.enabled=true
app.oracle.url=jdbc:oracle:thin:@localhost:1522/XEPDB1
app.oracle.username=ems
app.oracle.password=ems
```

Endpoint: `GET /api/budzet/oracle/upozorenja/{budzetId}`  
Implementacija: `BudzetKontrolaPlsqlDao` → `PKG_BUDZET_KONTROLA.PR_OBRADI_BUDZET`

Po defaultu `app.oracle.enabled=false` — aplikacija radi kao ranije samo preko MySQL.

---

## Scenarij odbrane (~5 min)

1. `08_f3_run_demo.sql` — procedura + REF CURSOR, 4 alert nivoa (ili `08_f3_run_demo_interactive.sql` u SQL Developeru)
2. `INSERT INTO troskovi` — compound trigger, nema ORA-04091
3. Objašnjenje `FOR UPDATE` — konkurentni troškovi
4. `EXPLAIN PLAN` za upit u `fn_neto_trosak_stavka`
5. Poziv na DRAFT budžet — exception `-20001`

---

## Pasus za rad / asistenta

Proceduralni deo baze za F3 realizovan je u Oracle PL/SQL modulu `PKG_BUDZET_KONTROLA`, paralelno sa postojećom Java/MySQL implementacijom iste poslovne logike. Modul je fizički odvojen u `database/oracle/f3/` i ne menja postojeću aplikaciju.
