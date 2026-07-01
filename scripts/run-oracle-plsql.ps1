# Pokreće Oracle PL/SQL modul preko Docker kontejnera (sqlplus).
# Upotreba:
#   .\scripts\run-oracle-plsql.ps1              # F5 modul (podrazumevano)
#   .\scripts\run-oracle-plsql.ps1 -Module F3   # F3 budzetska kontrola
#   .\scripts\run-oracle-plsql.ps1 -OnlyTest    # samo F5 test blok

param(
    [ValidateSet("F5", "F3")]
    [string]$Module = "F5",
    [string]$ContainerName = "ems-oracle-xe",
    [string]$User = "ems",
    [string]$Password = "ems",
    [string]$Service = "XEPDB1",
    [int]$HostPort = 1522,
    [switch]$OnlyTest,
    [switch]$WaitForOracle
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$OracleDir = Join-Path $Root "database\oracle"
$F3Dir = Join-Path $OracleDir "f3"

if (-not (Test-Path $OracleDir)) {
    Write-Error "Folder database/oracle ne postoji."
}

function Test-ContainerRunning {
    $state = docker inspect -f '{{.State.Running}}' $ContainerName 2>$null
    return $state -eq "true"
}

function Wait-OracleReady {
    Write-Host "Čekam Oracle XE ($ContainerName)..." -ForegroundColor Yellow
    $max = 60
    for ($i = 1; $i -le $max; $i++) {
        if (-not (Test-ContainerRunning)) {
            Write-Host "Kontejner nije pokrenut. Pokreni: docker compose up -d oracle-xe"
            exit 1
        }
        $result = docker exec $ContainerName bash -c "echo 'SELECT 1 FROM dual;' | sqlplus -s $User/$Password@//localhost:1521/$Service" 2>&1
        if ($LASTEXITCODE -eq 0 -and ($result -match "1")) {
            Write-Host "Oracle je spreman." -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 5
        Write-Host "  ... pokušaj $i/$max"
    }
    Write-Error "Oracle nije postao na vreme."
}

function Invoke-SqlFile {
    param(
        [string]$FullPath,
        [string]$RemoteDir = "/tmp"
    )
    if (-not (Test-Path $FullPath)) {
        Write-Error "Fajl ne postoji: $FullPath"
    }
    $name = Split-Path $FullPath -Leaf
    Write-Host ">> $name" -ForegroundColor Cyan
    docker cp $FullPath "${ContainerName}:${RemoteDir}/$name"
    docker exec $ContainerName bash -c "cd $RemoteDir && sqlplus -s $User/$Password@//localhost:1521/$Service @$name"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Greška pri izvršavanju $name"
    }
}

if ($WaitForOracle) {
    Wait-OracleReady
} elseif (-not (Test-ContainerRunning)) {
    Write-Host "Kontejner $ContainerName nije pokrenut. Pokrećem docker compose..." -ForegroundColor Yellow
    Push-Location $Root
    docker compose up -d oracle-xe
    Pop-Location
    Wait-OracleReady
} else {
    Write-Host "Kontejner $ContainerName je aktivan." -ForegroundColor Green
}

if ($Module -eq "F3") {
    if (-not (Test-Path $F3Dir)) {
        Write-Error "Folder database/oracle/f3 ne postoji."
    }
    Write-Host "=== F3 modul (PKG_BUDZET_KONTROLA) ===" -ForegroundColor Magenta
    docker exec $ContainerName bash -c "mkdir -p /tmp/f3"
    $f3Files = @(
        "00_f3_prerequisite_check.sql",
        "00_f3_drop.sql",
        "01_f3_tables.sql",
        "02_f3_indeksi.sql",
        "03_f3_types.sql",
        "04_f3_functions.sql",
        "05_pkg_budzet_kontrola.pks",
        "05_pkg_budzet_kontrola.pkb",
        "06_f3_trg_trosak_cmp.sql",
        "07_f3_seed_demo.sql",
        "08_f3_run_demo.sql"
    )
    foreach ($f in $f3Files) {
        Invoke-SqlFile -FullPath (Join-Path $F3Dir $f) -RemoteDir "/tmp/f3"
    }
} elseif ($OnlyTest) {
    Invoke-SqlFile -FullPath (Join-Path $OracleDir "06_test_simulacija_klijenta.sql")
} else {
    $files = @(
        "00_bootstrap_standalone.sql",
        "01_indeksi.sql",
        "02_dogadjaj_koordinator.sql",
        "03_paket_profit_menadzment.pks",
        "04_paket_profit_menadzment.pkb",
        "05_trg_analiza_profit.sql",
        "06_test_simulacija_klijenta.sql"
    )
    foreach ($f in $files) {
        Invoke-SqlFile -FullPath (Join-Path $OracleDir $f)
    }
}

Write-Host ""
Write-Host "PL/SQL modul ($Module) je primenjen. Proveri output iznad (DBMS_OUTPUT)." -ForegroundColor Green
