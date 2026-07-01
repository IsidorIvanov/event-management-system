# F5/F6 test podaci — uputstvo za pokretanje

Ovaj vodič objašnjava kako da pokreneš E2E seed skriptu koja kreira kompletan poslovni tok za **F5 (analiza profitabilnosti)** i **F6 (finansijski izveštaji)**.

## Šta skripta radi

Skripta `seed-f5-f6-workflow.ps1` automatski prolazi kroz ceo workflow preko REST API-ja:

```
lokacija → događaj → sesija/govornik → tip karte/registracija (prihod B2C)
→ budžet + stavke → dobavljač + ugovor → nabavka
→ ulazna faktura + plaćanje → ručni trošak
→ izlazna faktura + plaćanje (prihod B2B)
→ status ZAVRSEN → F5 analiza → F6 izveštaji (PDF/Excel)
```

Na kraju ispisuje **Run ID**, **ID događaja**, **ID analize**, test naloge i generisane izveštaje.

**Provera konzistentnosti:** rezime poredi očekivane iznose (iz scenarija) sa stvarnim vrednostima iz F5 analize (`ukupanPrihod`, `ukupanTrosak`). Ako se ne slažu, skripta ispisuje `[RAZLIKA!]` i upozorenje — test ne pada, ali signalizira problem u backend agregaciji.

**Napomena za odbranu:** honorar govornika ulazi u F5 trošak preko modula govornika, ne kao `Trosak` entitet i ne tereti budžetske stavke. Izlazna faktura je B2B prihod (klijent/partner), ne model sponzorstva.

---

## Preduslovi

| Komponenta | Zahtev |
|------------|--------|
| **Backend** | Spring Boot aplikacija na `http://localhost:8080` |
| **MySQL** | Baza `event_management_db` (ista kao u `.env`) |
| **mysql CLI** | U PATH-u (npr. MySQL Shell / MySQL Client) |
| **PowerShell** | Windows PowerShell 5.1+ ili PowerShell 7+ |
| **`.env`** | U korenu projekta sa `DB_USERNAME` i `DB_PASSWORD` |

### 1. Pokreni backend

Iz korena projekta:

```powershell
mvn spring-boot:run
```

Proveri da API odgovara (npr. login endpoint na portu 8080).

### 2. Proveri `.env`

Skripta čita kredencijale iz `.env` u korenu projekta:

```env
DB_USERNAME=root
DB_PASSWORD=...
DB_URL=jdbc:mysql://localhost:3306/event_management_db?...
```

Ako `DB_URL` postoji, ime baze se parsira odatle; inače se koristi `event_management_db`.

### 3. Proveri da `mysql` radi

```powershell
mysql --version
```

Ako nije u PATH-u, možeš proslediti punu putanju:

```powershell
.\scripts\seed-f5-f6-workflow.ps1 -MySqlBin "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
```

---

## Pokretanje

Iz **korena projekta**:

```powershell
cd D:\Work\IIS-SISTEM-ZA-ORGANIZACIJU-DOGADJAJA\event-management-system
```

### Scenario GUBITAK (podrazumevano)

Događaj sa očekivanim rezultatom analize **NEPROFITABILAN** (~165k prihod, ~245k trošak):

```powershell
.\scripts\seed-f5-f6-workflow.ps1
```

### Scenario DOBITAK

Događaj sa očekivanim rezultatom analize **PROFITABILAN** (~440k prihod, ~113k trošak):

```powershell
.\scripts\seed-f5-f6-workflow.ps1 -Scenario DOBITAK
```

### Dodatni parametri

```powershell
.\scripts\seed-f5-f6-workflow.ps1 `
    -Scenario DOBITAK `
    -BaseUrl "http://localhost:8080" `
    -MySqlBin "mysql" `
    -MySqlDatabase "event_management_db"
```

| Parametar | Opis | Podrazumevano |
|-----------|------|---------------|
| `-Scenario` | `GUBITAK` ili `DOBITAK` | `GUBITAK` |
| `-BaseUrl` | URL backend API-ja | `http://localhost:8080` |
| `-MySqlBin` | Putanja do `mysql` executable | `mysql` |
| `-MySqlUser` | MySQL korisnik (ako ne koristi `.env`) | `DB_USERNAME` iz `.env` |
| `-MySqlPassword` | MySQL lozinka | `DB_PASSWORD` iz `.env` |
| `-MySqlDatabase` | Ime baze | `event_management_db` |
| `-SkipRegister` | Preskoči registraciju, samo login postojećih naloga iz tog run-a | isključeno |

---

## Šta očekivati na izlazu

Uspešan run završava blokom:

```
========================================
 F5/F6 WORKFLOW USPESNO ZAVRSEN (DOBITAK)
========================================

Run ID           : 20260628222557
Scenario         : DOBITAK
Dogadjaj ID      : 54 - F5/F6 DOBITAK Demo Konferencija ...
Analiza ID       : 9 (FINALIZOVANA)
Rezultat F5      : PROFITABILAN (marza 0.7432)
...
Test nalozi (lozinka: Test123!):
  fin -> f5f6.fin.20260628222557@test.local
  ...
F6 izvestaji:
  #24 PO_DOGADJAJU/PDF -> izvestaji/2026/06/izvestaj-24.pdf
  ...
```

**Zapamti:**
- **Run ID** — jedinstveni sufiks za taj run (email naloga, naziv događaja)
- **Dogadjaj ID** — za pregled u frontendu i API-ju
- **Analiza ID** — F5 finalizovana analiza
- **Lozinka svih test naloga:** `Test123!`

---

## Finansijski scenariji (pregled)

| Stavka | GUBITAK | DOBITAK |
|--------|---------|---------|
| Registracije (B2C) | 1 × 15.000 | 3 × 20.000 |
| Izlazna faktura (B2B naplaćeno) | 150.000 | 380.000 |
| Ulazna faktura | 120.000 | 55.000 |
| Ručni trošak | 45.000 | 18.000 |
| Honorar govornika | 80.000 | 40.000 |
| **Ukupan prihod F5** | **~165.000** | **~440.000** |
| **Ukupan trošak F5** | **~245.000** | **~113.000** |
| **Rezultat analize** | **NEPROFITABILAN** | **PROFITABILAN** |

Nabavka (commitovani trošak) se kreira u oba scenarija, ali u F5 analizi je **informativna** — ne ulazi u ukupan trošak.

---

## Pregled u frontendu

1. Pokreni frontend (`npm run dev` u `frontend/`, obično `http://localhost:5173`).
2. Uloguj se jednim od test naloga (npr. **fin**):
   - Email: `f5f6.fin.<RunId>@test.local`
   - Lozinka: `Test123!`
3. Otvori:
   - **Analiza profitabilnosti:** `/dashboard/analiza-profitabilnosti`
   - **Finansijski izveštaji:** `/dashboard/izvestaji`

Generisani PDF/Excel fajlovi se čuvaju u `storage/izvestaji/` (relativna putanja iz output-a skripte).

---

## Fajl `seed-f5-f6-workflow-status.sql`

**Ne moraš ručno da pokrećeš ovaj fajl** ako skripta uspešno koristi `mysql`.

API trenutno nema endpoint za prelazak događaja u status **OBJAVLJEN** i **ZAVRSEN**, pa skripta to radi direktno u bazi:

1. Posle kreiranja događaja → `OBJAVLJEN`
2. Posle faktura/plaćanja, pre F5 analize → `ZAVRSEN`

SQL fajl služi kao **referenca** za ručni rad samo ako `mysql` nije dostupan.

### Ručni UPDATE (fallback)

Ako skripta ispiše upozorenje *„mysql nije u PATH“*, izvrši u MySQL Workbench ili CLI **u dva koraka** (ne ceo fajl odjednom):

```sql
-- Korak 1: posle kreiranja događaja, pre registracija
UPDATE dogadjaj SET status = 'OBJAVLJEN' WHERE dogadjaj_id = <ID>;

-- Korak 2: posle faktura, pre F5 analize
UPDATE dogadjaj SET status = 'ZAVRSEN' WHERE dogadjaj_id = <ID>;
```

Zameni `<ID>` stvarnim `dogadjaj_id` iz output-a skripte.

---

## Uloge test naloga

| Uloga | Ključ u skripti | Tipična upotreba |
|-------|-----------------|------------------|
| Finansijski kontrolor | `fin` | Budžet, fakture, F5 analiza, F6 izveštaji |
| Menadžer događaja | `menadzer` | Odobravanje budžeta, finalizacija F5 analize |
| Koordinator programa | `program` | Sesija, govornik, tip karte |
| Učesnik | `ucesnik` (+2 za DOBITAK) | Registracija na događaj |
| Klijent (B2B) | `klijent` | Izlazna faktura / sponzorstvo |

---

## Rešavanje problema

### Backend ne odgovara

- Proveri da li je Spring Boot pokrenut na portu iz `.env` (`SERVER_PORT`, podrazumevano 8080).
- Proveri log aplikacije za greške pri startu.

### MySQL greška pri statusu događaja

- Proveri `DB_USERNAME` / `DB_PASSWORD` u `.env`.
- Proveri da baza `event_management_db` postoji i da backend može da se poveže.
- Ručno pokreni UPDATE iz sekcije iznad.

### Registracija korisnika 400 / konflikt emaila

- Svaki run koristi novi `RunId` u email adresama — pokreni skriptu ponovo; ne koristi isti run dva puta bez `-SkipRegister`.

### F5 analiza ne može da se kreira

- Događaj mora biti u statusu **ZAVRSEN**.
- Mora postojati finansijski tok (fakture, troškovi) za taj događaj.

### F6 izveštaj nije READY

- Proveri backend log — često font/PDF ili nedostaju podaci za izabrani tip izveštaja.
- Proveri da analiza/fakture postoje za period ili entitet koji izveštaj traži.

---

## Ponovno pokretanje

Svaki run kreira **nove** test naloge i **novi** događaj. Stari podaci ostaju u bazi — to je namerno, radi poređenja više run-ova.

Za čistu bazu pre testa koristi sopstveni backup/restore ili ručno brisanje demo zapisa (nije deo ove skripte).

---

## Povezani fajlovi

| Fajl | Namena |
|------|--------|
| `scripts/seed-f5-f6-workflow.ps1` | Glavna E2E seed skripta |
| `scripts/seed-f5-f6-workflow-status.sql` | Referenca za ručno ažuriranje statusa događaja |
| `.env` | Kredencijali za MySQL (i backend) |
| `storage/izvestaji/` | Generisani PDF/Excel izveštaji (F6) |
