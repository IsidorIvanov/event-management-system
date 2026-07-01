<#
.SYNOPSIS
    Sveobuhvatan E2E seed za F5 (analiza profitabilnosti) i F6 (finansijski izveštaji).

.DESCRIPTION
    Pokreće kompletan poslovni tok preko REST API-ja:
      lokacija → događaj → sesija/govornik → tip karte/registracija (prihod B2C)
      → budžet + stavke → dobavljač + ugovor → nabavka (commitovani trošak)
      → ulazna faktura + plaćanje (AUTO_ULAZNA trošak) → ručni trošak → honorar
      → izlazna faktura + plaćanje (prihod B2B) → F5 analiza → F6 izveštaji

    Zahtevi:
      - Backend na http://localhost:8080 (podrazumevano)
      - MySQL dostupan za ažuriranje statusa događaja (OBJAVLJEN → ZAVRSEN)
        ili ručno pokrenuti SQL iz seed-f5-f6-workflow-status.sql

.PARAMETER BaseUrl
    Bazni URL API-ja (podrazumevano http://localhost:8080).

.PARAMETER SkipRegister
    Koristi postojeće naloge iz -Credentials umesto registracije novih.

.PARAMETER Scenario
    GUBITAK (podrazumevano) ili DOBITAK — finansijski iznosi za ocekivani rezultat analize.

.EXAMPLE
    .\scripts\seed-f5-f6-workflow.ps1

.EXAMPLE
    .\scripts\seed-f5-f6-workflow.ps1 -Scenario DOBITAK

.EXAMPLE
    .\scripts\seed-f5-f6-workflow.ps1 -BaseUrl "http://localhost:8080" -MySqlBin "mysql"
#>
[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [ValidateSet("GUBITAK", "DOBITAK")]
    [string]$Scenario = "GUBITAK",
    [switch]$SkipRegister,
    [string]$MySqlBin = "mysql",
    [string]$MySqlUser = "",
    [string]$MySqlPassword = "",
    [string]$MySqlDatabase = "event_management_db"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

# ---------------------------------------------------------------------------
# Konfiguracija
# ---------------------------------------------------------------------------
$RunId = Get-Date -Format "yyyyMMddHHmmss"
$Password = "Test123!"
# MetodPlacanja enum u backendu: BANKOVNI_TRANSFER, GOTOVINA, TRANSFER, KARTICA, ...
$MetodPlacanja = "BANKOVNI_TRANSFER"

$dayOffset = ([int]$RunId.Substring($RunId.Length - 4)) % 250 + 80
$EventStart = (Get-Date).AddDays(-$dayOffset).ToString("yyyy-MM-dd")
$EventEnd   = (Get-Date).AddDays(-($dayOffset - 2)).ToString("yyyy-MM-dd")
$FinDate    = (Get-Date).AddDays(-($dayOffset - 1)).ToString("yyyy-MM-dd")
# PeriodOd je 15 dana pre pocetka dogadjaja — pokriva EventStart i FinDate za F6 PO_PERIODU
$PeriodOd   = (Get-Date).AddDays(-($dayOffset + 15)).ToString("yyyy-MM-dd")
$PeriodDo   = (Get-Date).ToString("yyyy-MM-dd")

$FinProfiles = @{
    GUBITAK = @{
        eventLabel       = "GUBITAK Demo Konferencija"
        honorar          = "80000.00"
        kartaCena        = "15000.00"
        brojRegistracija  = 1
        ulaznaIznos      = "120000.00"
        rucniIznos       = "45000.00"
        nabavkaStavke    = @(
            @{ nazivResursa = "LED ekran 4x3m"; kolicina = 2; jedinicnaCena = "35000.00"; opis = "AV oprema" }
            @{ nazivResursa = "Zvucni sistem"; kolicina = 1; jedinicnaCena = "25000.00"; opis = "PA sistem" }
        )
        izlaznaIznos     = "200000.00"
        izlaznaPlaceno   = "150000.00"
        planiraniBudzet  = "500000.00"
    }
    DOBITAK = @{
        eventLabel       = "DOBITAK Demo Konferencija"
        honorar          = "40000.00"
        kartaCena        = "20000.00"
        brojRegistracija  = 3
        ulaznaIznos      = "55000.00"
        rucniIznos       = "18000.00"
        nabavkaStavke    = @(
            @{ nazivResursa = "Projektor set"; kolicina = 2; jedinicnaCena = "15000.00"; opis = "Projekcija" }
            @{ nazivResursa = "WiFi hub"; kolicina = 1; jedinicnaCena = "20000.00"; opis = "Mreza" }
        )
        izlaznaIznos     = "380000.00"
        izlaznaPlaceno   = "380000.00"
        planiraniBudzet  = "600000.00"
    }
}
$Fin = $FinProfiles[$Scenario]

$State = @{
    runId          = $RunId
    tokens         = @{}
    lokacijaId     = $null
    dogadjajId     = $null
    sesijaId       = $null
    govornikId     = $null
    tipKarte       = "Standard"
    kategorijaId   = $null
    budzetId       = $null
    dobavljacId    = $null
    ugovorId       = $null
    nabavkaId      = $null
    ulaznaFakturaId = $null
    izlaznaFakturaId = $null
    klijentId      = $null
    analizaId      = $null
    nazivSale      = $null
    izvestaji      = @()
}

# ---------------------------------------------------------------------------
# Pomoćne funkcije
# ---------------------------------------------------------------------------
function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Ok([string]$Message) {
    Write-Host "    OK: $Message" -ForegroundColor Green
}

function Write-Warn([string]$Message) {
    Write-Host "    UPOZORENJE: $Message" -ForegroundColor Yellow
}

function Format-Rsd([decimal]$Amount) {
    return $Amount.ToString("N0")
}

function Assert-FinansijskaSaglasnost {
    param(
        [string]$Label,
        [decimal]$Ocekivano,
        [decimal]$ApiVrednost
    )
    $delta = [Math]::Abs($Ocekivano - $ApiVrednost)
    if ($delta -gt [decimal]0.01) {
        Write-Warn "$Label se NE SLaze: ocekivano $(Format-Rsd $Ocekivano) RSD, API $(Format-Rsd $ApiVrednost) RSD (razlika $(Format-Rsd $delta))"
        return $false
    }
    return $true
}

function Load-DotEnv {
    $envFile = Join-Path (Split-Path $PSScriptRoot -Parent) ".env"
    if (-not (Test-Path $envFile)) { return }
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $val = $matches[2].Trim().Trim('"')
            if (-not [string]::IsNullOrWhiteSpace($key) -and -not (Get-Item -Path "Env:$key" -ErrorAction SilentlyContinue)) {
                Set-Item -Path "Env:$key" -Value $val
            }
        }
    }
}

function Parse-DbUrl {
    param([string]$JdbcUrl)
    if ($JdbcUrl -match 'jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)') {
        return @{
            Host = $matches[1]
            Port = if ($matches[2]) { $matches[2] } else { "3306" }
            Database = $matches[3]
        }
    }
    return $null
}

function Invoke-Api {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $null,
        [switch]$RawBytes
    )

    $uri = "$BaseUrl$Path"
    $headers = @{ Accept = "application/json" }
    if ($Token) { $headers.Authorization = "Bearer $Token" }

    $params = @{
        Uri         = $uri
        Method      = $Method
        Headers     = $headers
        ErrorAction = "Stop"
        TimeoutSec  = 120
    }

    if ($Body -ne $null) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = ($Body | ConvertTo-Json -Depth 12 -Compress)
    }

    try {
        if ($RawBytes) {
            return Invoke-WebRequest @params
        }
        $response = Invoke-RestMethod @params
        return $response
    }
    catch {
        $detail = $_.Exception.Message
        if ($null -ne $_.Exception.Response) {
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $bodyText = $reader.ReadToEnd()
                $reader.Close()
                if ($bodyText) {
                    try {
                        $errJson = $bodyText | ConvertFrom-Json
                        if ($errJson.error) { $detail = $errJson.error }
                        elseif ($errJson.message) { $detail = $errJson.message }
                    }
                    catch { $detail = $bodyText }
                }
            }
            catch { }
        }
        if ($null -ne $_.ErrorDetails -and $_.ErrorDetails.Message) {
            try {
                $errJson = $_.ErrorDetails.Message | ConvertFrom-Json
                if ($errJson.error) { $detail = $errJson.error }
                elseif ($errJson.message) { $detail = $errJson.message }
            }
            catch { $detail = $_.ErrorDetails.Message }
        }
        throw "API $Method $Path -> $detail"
    }
}

function Register-User {
    param(
        [string]$RoleKey,
        [hashtable]$Payload
    )
    $resp = Invoke-Api -Method POST -Path "/api/auth/register" -Body $Payload
    $State.tokens[$RoleKey] = $resp.token
    Write-Ok "Registrovan $($Payload.email) ($RoleKey)"
    return $resp
}

function Login-User {
    param(
        [string]$RoleKey,
        [string]$Email,
        [string]$Lozinka = $Password
    )
    $resp = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{
        email   = $Email
        lozinka = $Lozinka
    }
    $State.tokens[$RoleKey] = $resp.token
    Write-Ok "Prijavljen $Email ($RoleKey)"
    return $resp
}

function Token([string]$RoleKey) {
    return $State.tokens[$RoleKey]
}

function Set-DogadjajStatusViaMySql {
    param(
        [long]$DogadjajId,
        [ValidateSet("OBJAVLJEN", "ZAVRSEN")]
        [string]$Status
    )

    Load-DotEnv
    $user = if ($MySqlUser) { $MySqlUser } else { $env:DB_USERNAME }
    $pass = if ($MySqlPassword) { $MySqlPassword } else { $env:DB_PASSWORD }
    $db   = $MySqlDatabase
    if ($env:DB_URL) {
        $parsed = Parse-DbUrl $env:DB_URL
        if ($parsed) { $db = $parsed.Database }
    }

    if (-not $user) {
        throw "Nema DB_USERNAME u .env - potrebno za status dogadjaja ($Status)."
    }

    $mysql = Get-Command $MySqlBin -ErrorAction SilentlyContinue
    if (-not $mysql) {
        Write-Warn "mysql nije u PATH. Ručno izvrši: UPDATE dogadjaj SET status='$Status' WHERE dogadjaj_id=$DogadjajId;"
        return
    }

    $sql = "UPDATE dogadjaj SET status='$Status' WHERE dogadjaj_id=$DogadjajId;"
    $args = @("-u$user", "-h127.0.0.1", $db, "-e", $sql)
    if ($pass) { $env:MYSQL_PWD = $pass }

    & $MySqlBin @args | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "MySQL greška pri postavljanju statusa $Status" }
    Write-Ok "Događaj #$DogadjajId -> $Status (MySQL)"
}

# ---------------------------------------------------------------------------
# 0) Provera backend-a
# ---------------------------------------------------------------------------
Write-Step "Provera backend-a ($BaseUrl)"
try {
    Invoke-WebRequest -Uri "$BaseUrl/api/auth/login" -Method POST `
        -ContentType "application/json" `
        -Body '{"email":"probe@test.local","lozinka":"x"}' `
        -TimeoutSec 10 -ErrorAction Stop | Out-Null
}
catch {
    $code = $null
    if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
    if ($code -notin 400, 401, 403) {
        throw "Backend nije dostupan na $BaseUrl (HTTP $code). Pokreni: ./mvnw spring-boot:run"
    }
}
Write-Ok "Backend odgovara"

# ---------------------------------------------------------------------------
# 1) Korisnici
# ---------------------------------------------------------------------------
Write-Step "Korisnici (test nalozi za run $RunId, scenario $Scenario)"

$emails = @{
    fin       = "f5f6.fin.$RunId@test.local"
    menadzer  = "f5f6.menadzer.$RunId@test.local"
    program   = "f5f6.program.$RunId@test.local"
    ucesnik   = "f5f6.ucesnik.$RunId@test.local"
    ucesnik2  = "f5f6.ucesnik2.$RunId@test.local"
    ucesnik3  = "f5f6.ucesnik3.$RunId@test.local"
    klijent   = "f5f6.klijent.$RunId@test.local"
}

if ($SkipRegister) {
    foreach ($key in $emails.Keys) {
        Login-User -RoleKey $key -Email $emails[$key] | Out-Null
    }
}
else {
    Register-User "fin" @{
        ime = "Fin"; prezime = "Kontrolor"; email = $emails.fin; lozinka = $Password
        tipKorisnika = "ZAPOSLENI"; uloga = "FINANSIJSKI_KONTROLOR"; pozicija = "Finansije"
    } | Out-Null

    Register-User "menadzer" @{
        ime = "Marko"; prezime = "Menadzer"; email = $emails.menadzer; lozinka = $Password
        tipKorisnika = "ZAPOSLENI"; uloga = "MENADZER_DOGADJAJA"; pozicija = "Menadžer"
    } | Out-Null

    Register-User "program" @{
        ime = "Petar"; prezime = "Program"; email = $emails.program; lozinka = $Password
        tipKorisnika = "ZAPOSLENI"; uloga = "KOORDINATOR_PROGRAMA"; pozicija = "Koordinator"
    } | Out-Null

    Register-User "ucesnik" @{
        ime = "Ana"; prezime = "Ucesnik"; email = $emails.ucesnik; lozinka = $Password
        tipKorisnika = "UCESNIK"; kompanija = "Tech d.o.o."
        interesi = @("konferencije", "finansije")
    } | Out-Null

    if ($Fin.brojRegistracija -ge 2) {
        Register-User "ucesnik2" @{
            ime = "Bojan"; prezime = "Ucesnik"; email = $emails.ucesnik2; lozinka = $Password
            tipKorisnika = "UCESNIK"; kompanija = "Data d.o.o."
            interesi = @("konferencije")
        } | Out-Null
    }
    if ($Fin.brojRegistracija -ge 3) {
        Register-User "ucesnik3" @{
            ime = "Cana"; prezime = "Ucesnik"; email = $emails.ucesnik3; lozinka = $Password
            tipKorisnika = "UCESNIK"; kompanija = "Biz d.o.o."
            interesi = @("finansije")
        } | Out-Null
    }

    $klijentResp = Register-User "klijent" @{
        ime = "Sponsor"; prezime = "DOO"; email = $emails.klijent; lozinka = $Password
        tipKorisnika = "KLIJENT"; nazivFirme = "Sponsor Solutions d.o.o."
        pib = ($RunId.Substring($RunId.Length - 9)).PadLeft(9, '1')
        adresa = "Bulevar 1"; grad = "Beograd"; kontaktOsoba = "Milan Sponsor"
    }
    $State.klijentId = $klijentResp.korisnikId
}

if (-not $State.klijentId) {
    $klijenti = Invoke-Api -Path "/api/klijenti" -Token (Token "fin")
    $found = $klijenti | Where-Object { $_.email -eq $emails.klijent } | Select-Object -First 1
    if ($found) { $State.klijentId = $found.klijentId }
}

# ---------------------------------------------------------------------------
# 2) Lokacija
# ---------------------------------------------------------------------------
Write-Step "Lokacija"
$lokacije = Invoke-Api -Path "/api/lokacija" -Token (Token "program")
if ($lokacije -and $lokacije.Count -gt 0) {
    $State.lokacijaId = $lokacije[0].lokacijaId
    Write-Ok "Koristi postojecu lokaciju #$($State.lokacijaId) - $($lokacije[0].naziv)"
}
else {
    $nova = Invoke-Api -Method POST -Path "/api/lokacija" -Token (Token "menadzer") -Body @{
        naziv  = "F5/F6 Demo Centar $RunId"
        adresa = "Knez Mihailova 10"
        grad   = "Beograd"
        drzava = "Srbija"
    }
    $State.lokacijaId = $nova.lokacijaId
    Write-Ok "Kreirana lokacija #$($State.lokacijaId)"
}

# Sala mora postojati pre kreiranja sesije
$sale = Invoke-Api -Path "/api/sala?lokacijaId=$($State.lokacijaId)" -Token (Token "program")
$salaPostojeca = $sale | Where-Object { $_.nazivSale -eq "Sala A" } | Select-Object -First 1
if ($salaPostojeca) {
    $State.nazivSale = $salaPostojeca.nazivSale
    Write-Ok "Koristi salu '$($State.nazivSale)' na lokaciji #$($State.lokacijaId)"
}
else {
    $State.nazivSale = "F5F6-Sala-$RunId"
    Invoke-Api -Method POST -Path "/api/sala" -Token (Token "menadzer") -Body @{
        lokacijaId       = $State.lokacijaId
        nazivSale        = $State.nazivSale
        tipSale          = "GLAVNA"
        kapacitet        = 300
        baznaCenaPoDanu  = "5000.00"
    } | Out-Null
    Write-Ok "Kreirana sala '$($State.nazivSale)'"
}

# ---------------------------------------------------------------------------
# 3) Događaj (prošli termini — za ZAVRSEN i F6 period)
# ---------------------------------------------------------------------------
Write-Step "Događaj"
$dogadjaj = Invoke-Api -Method POST -Path "/api/dogadjaj" -Token (Token "program") -Body @{
    lokacijaId     = $State.lokacijaId
    naziv          = "F5/F6 $($Fin.eventLabel) $RunId"
    datumPocetka   = $EventStart
    datumZavrsetka = $EventEnd
    maksKapacitet  = 500
    opis           = "End-to-end demo događaj za analizu profitabilnosti i finansijske izveštaje."
    tagovi         = @("finansije", "demo", "konferencija")
}
$State.dogadjajId = $dogadjaj.dogadjajId
Write-Ok "Dogadjaj #$($State.dogadjajId) - $EventStart do $EventEnd (status DRAFT)"

Set-DogadjajStatusViaMySql -DogadjajId $State.dogadjajId -Status "OBJAVLJEN"

# ---------------------------------------------------------------------------
# 4) Program: sesija + govornik (honorar)
# ---------------------------------------------------------------------------
Write-Step "Sesija i govornik (honorar)"
$sesija = Invoke-Api -Method POST -Path "/api/sesija" -Token (Token "program") -Body @{
    dogadjajId     = $State.dogadjajId
    lokacijaId     = $State.lokacijaId
    nazivSale      = $State.nazivSale
    naziv          = "Keynote: Finansijska analitika događaja"
    datum          = $EventStart
    vremePocetka   = "10:00:00"
    vremeZavrsetka = "11:30:00"
    tip            = "KEYNOTE"
    kapacitet      = 300
    opis           = "Uvodi u F5/F6 workflow."
}
$State.sesijaId = $sesija.sesijaId

$govornik = Invoke-Api -Method POST -Path "/api/govornik" -Token (Token "program") -Body @{
    ime       = "Jelena"
    prezime   = "Analystic"
    kompanija = "Event Finance Lab"
    pozicija  = "Glavni govornik"
    email     = "govornik.$RunId@test.local"
    honorar   = $Fin.honorar
}
$State.govornikId = $govornik.govornikId

Invoke-Api -Method POST -Path "/api/sesija/$($State.sesijaId)/govornici/$($State.govornikId)" -Token (Token "program") | Out-Null
Write-Ok "Sesija #$($State.sesijaId), govornik #$($State.govornikId) honorar $($Fin.honorar) RSD"

# ---------------------------------------------------------------------------
# 5) Tip karte + registracija (prihod B2C)
# ---------------------------------------------------------------------------
Write-Step "Tip karte i registracija ucesnika (prihod B2C)"
Invoke-Api -Method POST -Path "/api/tip-karte" -Token (Token "program") -Body @{
    dogadjajId  = $State.dogadjajId
    nazivTipa   = $State.tipKarte
    vrsta       = "VISEDNEVNA"
    cena        = $Fin.kartaCena
    kvota       = 200
    opis        = "Puna ulaznica za demo konferenciju"
} | Out-Null

$ucesnikKeys = @("ucesnik")
if ($Fin.brojRegistracija -ge 2) { $ucesnikKeys += "ucesnik2" }
if ($Fin.brojRegistracija -ge 3) { $ucesnikKeys += "ucesnik3" }
foreach ($uk in $ucesnikKeys) {
    Invoke-Api -Method POST -Path "/api/registracija" -Token (Token $uk) -Body @{
        dogadjajId  = $State.dogadjajId
        nazivTipa   = $State.tipKarte
    } | Out-Null
}
$b2cPrihod = [decimal]$Fin.kartaCena * $Fin.brojRegistracija
Write-Ok "$($Fin.brojRegistracija) registracija x $($Fin.kartaCena) RSD = $b2cPrihod RSD prihoda (B2C)"

# ---------------------------------------------------------------------------
# 6) Budžet + kategorija + stavke
# ---------------------------------------------------------------------------
Write-Step "Budžet i stavke"
$kat = Invoke-Api -Method POST -Path "/api/budzet/kategorije" -Token (Token "fin") -Body @{
    naziv = "Oprema i usluge F5F6 $RunId"
    opis  = "Kategorija za demo nabavku i ulazne fakture"
}
$State.kategorijaId = $kat.kategorijaId

$katMarketing = Invoke-Api -Method POST -Path "/api/budzet/kategorije" -Token (Token "fin") -Body @{
    naziv = "Marketing F5F6 $RunId"
    opis  = "Ručni troškovi promocije"
}

$budzet = Invoke-Api -Method POST -Path "/api/budzet" -Token (Token "fin") -Body @{
    dogadjajId      = $State.dogadjajId
    nazivBudzeta    = "Operativni budžet F5/F6 $RunId"
    planiraniIznos  = $Fin.planiraniBudzet
}
$State.budzetId = $budzet.budzetId

Invoke-Api -Method POST -Path "/api/budzet/$($State.budzetId)/stavke" -Token (Token "fin") -Body @{
    kategorijaId    = $State.kategorijaId
    planiraniIznos  = "300000.00"
    pragUpozorenja  = "0.7500"
    pragKriticnog   = "0.9500"
} | Out-Null

Invoke-Api -Method POST -Path "/api/budzet/$($State.budzetId)/stavke" -Token (Token "fin") -Body @{
    kategorijaId    = $katMarketing.kategorijaId
    planiraniIznos  = "80000.00"
    pragUpozorenja  = "0.8000"
    pragKriticnog   = "0.9500"
} | Out-Null

Invoke-Api -Method POST -Path "/api/budzet/$($State.budzetId)/approve" -Token (Token "menadzer") | Out-Null
Invoke-Api -Method POST -Path "/api/budzet/$($State.budzetId)/activate" -Token (Token "fin") | Out-Null
Write-Ok "Budzet #$($State.budzetId) odobren i aktivan ($($Fin.planiraniBudzet) RSD)"

# ---------------------------------------------------------------------------
# 7) Dobavljač + ugovor
# ---------------------------------------------------------------------------
Write-Step "Dobavljač i ugovor"
$dob = Invoke-Api -Method POST -Path "/api/nabavka/dobavljac" -Token (Token "menadzer") -Body @{
    naziv         = "Demo Catering & AV $RunId"
    grad          = "Beograd"
    kontaktEmail  = "dobavljac.$RunId@test.local"
    telefon       = "+381601234567"
    pib           = ($RunId.Substring($RunId.Length - 9)).PadLeft(9, '2')
    rejting       = "4.50"
    kriterijumi   = "Ketering,AV oprema"
}
$State.dobavljacId = $dob.dobavljacId

$ugovor = Invoke-Api -Method POST -Path "/api/ugovori" -Token (Token "menadzer") -Body @{
    brojUgovora       = "UG-F5F6-$RunId"
    dobavljacId       = $State.dobavljacId
    dogadjajId        = $State.dogadjajId
    datumPotpisivanja = $EventStart
    vaziDo            = (Get-Date).AddYears(1).ToString("yyyy-MM-dd")
    vrednost          = "350000.00"
    predmet           = "Ketering i tehnička oprema za demo konferenciju"
    usloviPlacanja    = "30 dana od izdavanja fakture"
}
$State.ugovorId = $ugovor.ugovorId

Invoke-Api -Method PUT -Path "/api/ugovori/$($State.ugovorId)/dokument" -Token (Token "menadzer") -Body @{
    dokumentUrl = "https://example.local/ugovori/UG-F5F6-$RunId.pdf"
} | Out-Null

Invoke-Api -Method PUT -Path "/api/ugovori/$($State.ugovorId)/aktiviraj" -Token (Token "menadzer") | Out-Null
Write-Ok "Dobavljač #$($State.dobavljacId), ugovor #$($State.ugovorId) AKTIVAN"

# ---------------------------------------------------------------------------
# 8) Nabavka (commitovani trošak — bez ulazne fakture)
# ---------------------------------------------------------------------------
Write-Step "Nabavka (commitovani trošak u F5)"
$nabavka = Invoke-Api -Method POST -Path "/api/nabavka" -Token (Token "fin") -Body @{
    dogadjajId = $State.dogadjajId
    stavke     = $Fin.nabavkaStavke
}
$State.nabavkaId = $nabavka.nabavkaId

Invoke-Api -Method POST -Path "/api/nabavka/$($State.nabavkaId)/kontakt/$($State.dobavljacId)" -Token (Token "fin") | Out-Null
Invoke-Api -Method PATCH -Path "/api/nabavka/$($State.nabavkaId)/status" -Token (Token "fin") -Body @{
    status = "POTVRDJENA"
} | Out-Null
$nabavkaCommit = ($Fin.nabavkaStavke | ForEach-Object { [decimal]$_.jedinicnaCena * [int]$_.kolicina } | Measure-Object -Sum).Sum
Write-Ok "Nabavka #$($State.nabavkaId) POTVRDJENA - commitovani trosak $nabavkaCommit RSD (informativno u F5)"

# ---------------------------------------------------------------------------
# 9) Ulazna faktura → AUTO_ULAZNA trošak + plaćanje
# ---------------------------------------------------------------------------
Write-Step "Ulazna faktura (ketering) + placanje"
$ulazna = Invoke-Api -Method POST -Path "/api/fakture" -Token (Token "fin") -Body @{
    brojFakture     = "UL-F5F6-$RunId"
    tip             = "ULAZNA"
    dogadjajId      = $State.dogadjajId
    dobavljacId     = $State.dobavljacId
    ugovorId        = $State.ugovorId
    datumIzdavanja  = $FinDate
    rokPlacanja     = $FinDate
    napomena        = "Ketering za učesnike demo konferencije"
}
$State.ulaznaFakturaId = $ulazna.fakturaId

Invoke-Api -Method POST -Path "/api/fakture/$($State.ulaznaFakturaId)/stavke" -Token (Token "fin") -Body @{
    naziv         = "Ketering - rucak i kafa pauza"
    kolicina      = "1"
    jedinicnaCena = $Fin.ulaznaIznos
    budzetId      = $State.budzetId
    kategorijaId  = $State.kategorijaId
} | Out-Null

Invoke-Api -Method POST -Path "/api/fakture/$($State.ulaznaFakturaId)/issue" -Token (Token "fin") | Out-Null

$plUlazna = Invoke-Api -Method POST -Path "/api/placanja/faktura/$($State.ulaznaFakturaId)" -Token (Token "fin") -Body @{
    iznos           = $Fin.ulaznaIznos
    datumPlacanja   = $FinDate
    metod           = $MetodPlacanja
    referentniBroj  = "PL-UL-$RunId"
}
Invoke-Api -Method POST -Path "/api/placanja/$($plUlazna.placanjeId)/confirm" -Token (Token "fin") | Out-Null
Write-Ok "Ulazna faktura #$($State.ulaznaFakturaId) $($Fin.ulaznaIznos) RSD placena -> AUTO_ULAZNA trosak"

# ---------------------------------------------------------------------------
# 10) Ručni trošak (marketing)
# ---------------------------------------------------------------------------
Write-Step "Rucni trosak (marketing)"
Invoke-Api -Method POST -Path "/api/troskovi" -Token (Token "fin") -Body @{
    budzetId      = $State.budzetId
    kategorijaId  = $katMarketing.kategorijaId
    dogadjajId    = $State.dogadjajId
    opis          = "Online kampanja i štampa plakata"
    iznos         = $Fin.rucniIznos
    datumTroska   = $FinDate
    tip           = "RUCNI"
} | Out-Null
Write-Ok "Rucni trosak $($Fin.rucniIznos) RSD (datumTroska=$FinDate)"

# ---------------------------------------------------------------------------
# 11) Izlazna faktura (B2B prihod) + delimično plaćanje
# ---------------------------------------------------------------------------
Write-Step "Izlazna faktura (B2B partner) + placanje"
$izlazna = Invoke-Api -Method POST -Path "/api/fakture" -Token (Token "fin") -Body @{
    brojFakture     = "IZ-F5F6-$RunId"
    tip             = "IZLAZNA"
    dogadjajId      = $State.dogadjajId
    klijentId       = $State.klijentId
    datumIzdavanja  = $FinDate
    rokPlacanja     = $FinDate
    napomena        = "B2B partnerski paket usluga - Gold"
}
$State.izlaznaFakturaId = $izlazna.fakturaId

Invoke-Api -Method POST -Path "/api/fakture/$($State.izlaznaFakturaId)/stavke" -Token (Token "fin") -Body @{
    naziv         = "Partnerski paket usluga - Gold (B2B)"
    kolicina      = "1"
    jedinicnaCena = $Fin.izlaznaIznos
    budzetId      = $State.budzetId
    kategorijaId  = $State.kategorijaId
} | Out-Null

Invoke-Api -Method POST -Path "/api/fakture/$($State.izlaznaFakturaId)/issue" -Token (Token "fin") | Out-Null

$plIzlazna = Invoke-Api -Method POST -Path "/api/placanja/faktura/$($State.izlaznaFakturaId)" -Token (Token "fin") -Body @{
    iznos           = $Fin.izlaznaPlaceno
    datumPlacanja   = $FinDate
    metod           = $MetodPlacanja
    referentniBroj  = "PL-IZ-$RunId"
}
Invoke-Api -Method POST -Path "/api/placanja/$($plIzlazna.placanjeId)/confirm" -Token (Token "fin") | Out-Null
Write-Ok "Izlazna faktura #$($State.izlaznaFakturaId) fakturisano $($Fin.izlaznaIznos), naplaceno $($Fin.izlaznaPlaceno) RSD (B2B prihod)"

# ---------------------------------------------------------------------------
# 12) Završi događaj (F5 zahteva ZAVRSEN)
# ---------------------------------------------------------------------------
Write-Step "Zavrsetak dogadjaja (status ZAVRSEN)"
Set-DogadjajStatusViaMySql -DogadjajId $State.dogadjajId -Status "ZAVRSEN"

# ---------------------------------------------------------------------------
# 13) F5 — Analiza profitabilnosti
# ---------------------------------------------------------------------------
Write-Step "F5 - Analiza profitabilnosti"
$analiza = Invoke-Api -Method POST -Path "/api/analiza/profitabilnost/dogadjaj/$($State.dogadjajId)" -Token (Token "fin") -Body @{
    napomene = "Automatski generisana analiza iz seed-f5-f6-workflow.ps1 (run $RunId)"
}
$State.analizaId = $analiza.analizaId
Write-Ok "Draft analiza #$($State.analizaId) - prihod $($analiza.ukupanPrihod), trosak $($analiza.ukupanTrosak), rezultat $($analiza.rezultatOcene)"

$final = Invoke-Api -Method PUT -Path "/api/analiza/profitabilnost/$($State.analizaId)/finalizuj" -Token (Token "menadzer")
Write-Ok "Finalizovana analiza - rezultat: $($final.rezultatOcene), marza $($final.marza)"

# ---------------------------------------------------------------------------
# 14) F6 — Finansijski izveštaji (svi tipovi)
# ---------------------------------------------------------------------------
Write-Step "F6 - Generisanje finansijskih izvestaja"

$izvestajConfigs = @(
    @{
        label = "PO_DOGADJAJU (PDF)"
        body  = @{
            tip = "PO_DOGADJAJU"; format = "PDF"; dogadjajId = $State.dogadjajId
        }
    },
    @{
        label = "PO_KLIJENTU (PDF)"
        body  = @{
            tip = "PO_KLIJENTU"; format = "PDF"
            klijentId = $State.klijentId
            periodOd = $PeriodOd; periodDo = $PeriodDo
        }
    },
    @{
        label = "PO_DOBAVLJACU (Excel)"
        body  = @{
            tip = "PO_DOBAVLJACU"; format = "EXCEL"
            dobavljacId = $State.dobavljacId
            periodOd = $PeriodOd; periodDo = $PeriodDo
        }
    },
    @{
        label = "PO_PERIODU (PDF)"
        body  = @{
            tip = "PO_PERIODU"; format = "PDF"
            periodOd = $PeriodOd; periodDo = $PeriodDo
        }
    }
)

foreach ($cfg in $izvestajConfigs) {
    $iz = Invoke-Api -Method POST -Path "/api/izvestaji" -Token (Token "fin") -Body $cfg.body
    if ($iz.status -ne "READY") {
        throw "Izvestaj '$($cfg.label)' nije READY - status: $($iz.status), greska: $($iz.greskaPoruka)"
    }
    $State.izvestaji += $iz
    Write-Ok "$($cfg.label) -> izvestaj #$($iz.izvestajId) ($($iz.fileUrl))"
}

$State.finalAnaliza = $final

# ---------------------------------------------------------------------------
# Rezime
# ---------------------------------------------------------------------------
$b2cPrihodSum = [decimal]$Fin.kartaCena * $Fin.brojRegistracija
$b2bPrihodSum = [decimal]$Fin.izlaznaPlaceno
$ocekivaniPrihod = $b2cPrihodSum + $b2bPrihodSum
$ocekivaniTrosak = [decimal]$Fin.ulaznaIznos + [decimal]$Fin.rucniIznos + [decimal]$Fin.honorar
$apiPrihod = [decimal]$final.ukupanPrihod
$apiTrosak = [decimal]$final.ukupanTrosak
$nabavkaCommitSum = ($Fin.nabavkaStavke | ForEach-Object { [decimal]$_.jedinicnaCena * [int]$_.kolicina } | Measure-Object -Sum).Sum

$prihodOk = Assert-FinansijskaSaglasnost -Label "Ukupan prihod F5" -Ocekivano $ocekivaniPrihod -ApiVrednost $apiPrihod
$trosakOk = Assert-FinansijskaSaglasnost -Label "Ukupan trosak F5" -Ocekivano $ocekivaniTrosak -ApiVrednost $apiTrosak
$saglasnostOk = $prihodOk -and $trosakOk

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host " F5/F6 WORKFLOW USPESNO ZAVRSEN ($Scenario)" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""
Write-Host "Run ID           : $RunId"
Write-Host "Scenario         : $Scenario"
Write-Host "Dogadjaj ID      : $($State.dogadjajId) - F5/F6 $($Fin.eventLabel) $RunId"
Write-Host "Budzet ID        : $($State.budzetId)"
Write-Host "Analiza ID       : $($State.analizaId) (FINALIZOVANA)"
Write-Host "Rezultat F5      : $($final.rezultatOcene) (marza $($final.marza))"
Write-Host ""
Write-Host "Finansijski tok (ocekivano vs API F5):" -ForegroundColor White
Write-Host "  Prihod B2C karte       : $(Format-Rsd $b2cPrihodSum) RSD ($($Fin.brojRegistracija) x $($Fin.kartaCena))"
Write-Host "  Prihod B2B izlazna     : $(Format-Rsd $b2bPrihodSum) RSD"
Write-Host "  Ukupan prihod F5       : ocekivano $(Format-Rsd $ocekivaniPrihod) | API $(Format-Rsd $apiPrihod) $(if ($prihodOk) { '[OK]' } else { '[RAZLIKA!]' })"
Write-Host "  Trosak AUTO_ULAZNA     : $(Format-Rsd ([decimal]$Fin.ulaznaIznos)) RSD"
Write-Host "  Trosak RUCNI           : $(Format-Rsd ([decimal]$Fin.rucniIznos)) RSD"
Write-Host "  Honorar govornika      : $(Format-Rsd ([decimal]$Fin.honorar)) RSD (modul govornika, ne Trosak entitet)"
Write-Host "  Ukupan trosak F5       : ocekivano $(Format-Rsd $ocekivaniTrosak) | API $(Format-Rsd $apiTrosak) $(if ($trosakOk) { '[OK]' } else { '[RAZLIKA!]' })"
Write-Host "  Commitovani nabavka    : $(Format-Rsd $nabavkaCommitSum) RSD (informativno, ne ulazi u F5 trosak)"
if (-not $saglasnostOk) {
    Write-Host ""
    Write-Warn "Finansijska provera: ocekivane vrednosti skripte se razlikuju od F5 analize — proveri backend agregaciju pre odbrane."
}
Write-Host ""
Write-Host "Test nalozi (lozinka: $Password):" -ForegroundColor White
$userKeys = @("fin", "menadzer", "program", "ucesnik")
if ($Fin.brojRegistracija -ge 2) { $userKeys += "ucesnik2" }
if ($Fin.brojRegistracija -ge 3) { $userKeys += "ucesnik3" }
$userKeys += "klijent"
foreach ($key in $userKeys) {
    Write-Host "  $key -> $($emails[$key])"
}
Write-Host ""
Write-Host "F6 izvestaji:" -ForegroundColor White
foreach ($iz in $State.izvestaji) {
    Write-Host "  #$($iz.izvestajId) $($iz.tip)/$($iz.format) -> $($iz.fileUrl)"
}
Write-Host ""
Write-Host "Frontend: /dashboard/analiza-profitabilnosti | /dashboard/izvestaji" -ForegroundColor DarkGray
Write-Host "Preuzimanje: GET $BaseUrl/api/izvestaji/{id}/download (Bearer fin token)" -ForegroundColor DarkGray
