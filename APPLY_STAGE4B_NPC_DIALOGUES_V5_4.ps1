$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$World = Join-Path $Root "run\saves\WASTED_TEST"
$SourceDir = Join-Path $Root "CUSTOMNPCS_STAGE4"

Write-Host "============================================================"
Write-Host "DomeSurvival Stage 4B V5.4 - NPC dialogue update"
Write-Host "============================================================"
Write-Host ""

if (-not (Test-Path -LiteralPath $World)) {
    throw "WASTED_TEST not found: $World"
}

$TargetDir = Join-Path $World "customnpcs\scripts\ecmascript"
New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$BackupDir = Join-Path $World "customnpcs\scripts\_domesurvival_backup\dialogues_v5_4_$Stamp"

$Files = @(
    "ambient_expedition_soldier.js",
    "ambient_security_officer.js"
)

foreach ($Name in $Files) {
    $Source = Join-Path $SourceDir $Name
    $Target = Join-Path $TargetDir $Name

    if (-not (Test-Path -LiteralPath $Source)) {
        throw "Missing source script: $Source"
    }

    if (Test-Path -LiteralPath $Target) {
        New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
        Copy-Item -LiteralPath $Target -Destination (Join-Path $BackupDir $Name) -Force
    }

    Copy-Item -LiteralPath $Source -Destination $Target -Force

    $SourceHash = (Get-FileHash -LiteralPath $Source -Algorithm SHA256).Hash
    $TargetHash = (Get-FileHash -LiteralPath $Target -Algorithm SHA256).Hash

    if ($SourceHash -ne $TargetHash) {
        throw "Verification failed: $Name"
    }

    Write-Host "[OK] $Name" -ForegroundColor Green
}

Write-Host ""
Write-Host "maneogflow: rap / recording ambient phrases"
Write-Host "iVan: Player's Club"
Write-Host ""
Write-Host "[OK] Dialogue scripts installed." -ForegroundColor Green
Write-Host "Restart FULL DEV, then Reset Script / Apply once on both NPCs."
exit 0
