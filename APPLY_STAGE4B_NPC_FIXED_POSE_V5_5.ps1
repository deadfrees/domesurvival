$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$SourceDir = Join-Path $Root "CUSTOMNPCS_STAGE4"

Write-Host "============================================================"
Write-Host "DomeSurvival Stage 4B V5.5 - FIXED NPC POSE"
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

# CustomNPCs ports may use either the game-level script folder or a world-local
# script folder. Install to the canonical game-level location and refresh the
# world-local location as well if it already exists.
$Targets = New-Object System.Collections.Generic.List[string]
$Targets.Add((Join-Path $Root "run\customnpcs\scripts\ecmascript"))

$WorldLocal = Join-Path $Root "run\saves\WASTED_TEST\customnpcs\scripts\ecmascript"
if (Test-Path -LiteralPath $WorldLocal) {
    $Targets.Add($WorldLocal)
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"

foreach ($TargetDir in $Targets) {
    New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null

    $BackupDir = Join-Path $TargetDir "_domesurvival_backup\fixed_pose_v5_5_$Stamp"

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
Write-Host "Fixed positions:"
Write-Host "  maneogflow  -508.950 62.0 -596.588"
Write-Host "  iVan        -534.938 62.0 -664.466"
Write-Host ""
Write-Host "Fixed yaw:"
Write-Host "  maneogflow  0.0"
Write-Host "  iVan        0.0"
Write-Host ""
Write-Host "[OK] Fixed-pose scripts installed." -ForegroundColor Green
Write-Host "Restart FULL DEV and Reset Script / Apply once on both NPCs."
exit 0
