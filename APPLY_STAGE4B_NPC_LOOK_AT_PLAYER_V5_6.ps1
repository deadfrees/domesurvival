$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$SourceDir = Join-Path $Root "CUSTOMNPCS_STAGE4"

Write-Host "============================================================"
Write-Host "DomeSurvival Stage 4B V5.6 - LOOK AT PLAYER"
Write-Host "============================================================"
Write-Host ""

$Names = @(
    "ambient_expedition_soldier.js",
    "ambient_security_officer.js"
)

foreach ($Name in $Names) {
    $Source = Join-Path $SourceDir $Name
    if (-not (Test-Path -LiteralPath $Source)) {
        throw "Missing source script: $Source"
    }
}

$Targets = New-Object System.Collections.Generic.List[string]

# Main CustomNPCs script directory used by the dev instance.
$Targets.Add((Join-Path $Root "run\customnpcs\scripts\ecmascript"))

# Also update the world-local directory if this CustomNPCs build created one.
$WorldLocal = Join-Path $Root "run\saves\WASTED_TEST\customnpcs\scripts\ecmascript"
if (Test-Path -LiteralPath $WorldLocal) {
    $Targets.Add($WorldLocal)
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"

foreach ($TargetDir in $Targets) {
    New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null
    $BackupDir = Join-Path $TargetDir "_domesurvival_backup\look_at_player_v5_6_$Stamp"

    foreach ($Name in $Names) {
        $Source = Join-Path $SourceDir $Name
        $Target = Join-Path $TargetDir $Name

        if (Test-Path -LiteralPath $Target) {
            New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
            Copy-Item -LiteralPath $Target -Destination (Join-Path $BackupDir $Name) -Force
        }

        Copy-Item -LiteralPath $Source -Destination $Target -Force

        $SourceHash = (Get-FileHash -LiteralPath $Source -Algorithm SHA256).Hash
        $TargetHash = (Get-FileHash -LiteralPath $Target -Algorithm SHA256).Hash

        if ($SourceHash -ne $TargetHash) {
            throw "Verification failed: $Target"
        }

        Write-Host "[OK] $Target" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "[OK] NPC look-at-player scripts installed." -ForegroundColor Green
Write-Host ""
Write-Host "Behavior:"
Write-Host "  - positions remain locked"
Write-Host "  - NPCs never walk"
Write-Host "  - nearest player inside 48 blocks controls facing direction"
Write-Host "  - clicking NPC immediately turns it toward that player"
Write-Host ""
Write-Host "Restart FULL DEV and Reset Script / Apply once on both NPCs."
exit 0
