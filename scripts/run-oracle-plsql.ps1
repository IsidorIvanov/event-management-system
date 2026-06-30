# Pokreće Oracle PL/SQL modul preko Docker kontejnera (sqlplus).
# Upotreba: .\scripts\run-oracle-plsql.ps1
#          .\scripts\run-oracle-plsql.ps1 -OnlyTest   # samo test blok (objekti već postoje)

param(
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
    param([string]$RelativePath)
    $full = Join-Path $OracleDir $RelativePath
    if (-not (Test-Path $full)) {
        Write-Error "Fajl ne postoji: $full"
    }
    $name = Split-Path $full -Leaf
    Write-Host ">> $name" -ForegroundColor Cyan
    docker cp $full "${ContainerName}:/tmp/$name"
    docker exec $ContainerName bash -c "cd /tmp && sqlplus -s $User/$Password@//localhost:1521/$Service @$name"
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

if ($OnlyTest) {
    Invoke-SqlFile "06_test_simulacija_klijenta.sql"
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
        Invoke-SqlFile $f
    }
}

Write-Host ""
Write-Host "PL/SQL modul je primenjen. Proveri output iznad (DBMS_OUTPUT)." -ForegroundColor Green
