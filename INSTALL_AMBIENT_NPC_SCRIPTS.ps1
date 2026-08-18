param(
    [Parameter(Mandatory=$false)]
    [string]$WorldPath
)

$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path $PSScriptRoot).Path

if ([string]::IsNullOrWhiteSpace($WorldPath)) {
    $DefaultWorld = Join-Path $ProjectRoot "run\saves\WASTED_TEST"

    if (Test-Path -LiteralPath $DefaultWorld) {
        $WorldPath = $DefaultWorld
    }
    else {
        throw @"
WorldPath was not supplied and the default world was not found:
  $DefaultWorld

Run:
  .\INSTALL_AMBIENT_NPC_SCRIPTS.bat "C:\path\to\world"
"@
    }
}

$WorldPath = (Resolve-Path -LiteralPath $WorldPath).Path

$SourceDir = Join-Path $ProjectRoot "CUSTOMNPCS_STAGE4"
$SecuritySource = Join-Path $SourceDir "ambient_security_officer.js"
$ExpeditionSource = Join-Path $SourceDir "ambient_expedition_soldier.js"

foreach ($Source in @($SecuritySource, $ExpeditionSource)) {
    if (-not (Test-Path -LiteralPath $Source)) {
        throw "NPC script source not found: $Source"
    }
}

$TargetDir = Join-Path $WorldPath "customnpcs\scripts\ecmascript"
New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$BackupDir = Join-Path $WorldPath "customnpcs\scripts\_domesurvival_backup\$Stamp"
$BackedUp = $false

foreach ($Name in @(
    "ambient_security_officer.js",
    "ambient_expedition_soldier.js"
)) {
    $Existing = Join-Path $TargetDir $Name

    if (Test-Path -LiteralPath $Existing) {
        New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
        Copy-Item -LiteralPath $Existing -Destination (Join-Path $BackupDir $Name) -Force
        $BackedUp = $true
    }
}

Copy-Item -LiteralPath $SecuritySource `
    -Destination (Join-Path $TargetDir "ambient_security_officer.js") `
    -Force

Copy-Item -LiteralPath $ExpeditionSource `
    -Destination (Join-Path $TargetDir "ambient_expedition_soldier.js") `
    -Force

$Pairs = @(
    @{
        Source = $SecuritySource
        Target = Join-Path $TargetDir "ambient_security_officer.js"
    },
    @{
        Source = $ExpeditionSource
        Target = Join-Path $TargetDir "ambient_expedition_soldier.js"
    }
)

foreach ($Pair in $Pairs) {
    $SourceHash = (Get-FileHash -LiteralPath $Pair.Source -Algorithm SHA256).Hash
    $TargetHash = (Get-FileHash -LiteralPath $Pair.Target -Algorithm SHA256).Hash

    if ($SourceHash -ne $TargetHash) {
        throw "Verification failed after copying: $($Pair.Target)"
    }
}

Write-Host ""
Write-Host "[OK] Ambient NPC scripts installed and SHA-256 verified." -ForegroundColor Green
Write-Host "World:  $WorldPath"
Write-Host "Target: $TargetDir"

if ($BackedUp) {
    Write-Host "Backup: $BackupDir"
}

Write-Host ""
Write-Host "In CustomNPCs:" -ForegroundColor Cyan
Write-Host "  1. Security NPC -> ambient_security_officer.js"
Write-Host "  2. Expedition NPC -> ambient_expedition_soldier.js"
Write-Host "  3. Press Save/Apply or Reset Script once for each NPC."
Write-Host ""
Write-Host "The scripts themselves set the exact position, name, title, skin and AI."
