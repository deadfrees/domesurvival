$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Joseph = Join-Path $Root "CUSTOMNPCS_STAGE4\joseph_cooper_gui.js"

if (-not (Test-Path -LiteralPath $Joseph)) {
    throw "Joseph GUI source not found: $Joseph"
}

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Js = [IO.File]::ReadAllText($Joseph, [Text.Encoding]::UTF8)

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\joseph_v742_stage1_key_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Joseph -Destination (Join-Path $Backup "joseph_cooper_gui.js") -Force

$Declaration = 'var STAGE1_PATH_UPGRADE_KEY = "domesurvival.stage01.path_upgraded.v733";'

# The runtime error proves the symbol is referenced but not declared.
# Add exactly one ES5/Nashorn-safe declaration without touching quest progress.
if (-not $Js.Contains($Declaration)) {
    $Anchor = 'function ensureStage1PathUpgrade(player) {'

    if (-not $Js.Contains($Anchor)) {
        throw "Could not find ensureStage1PathUpgrade(player) anchor."
    }

    $Js = $Js.Replace(
        $Anchor,
        $Declaration + "`r`n`r`n" + $Anchor
    )
}

# Safety validation: one declaration, at least one use.
$DeclCount = ([regex]::Matches(
    $Js,
    'var\s+STAGE1_PATH_UPGRADE_KEY\s*=\s*"domesurvival\.stage01\.path_upgraded\.v733"\s*;'
)).Count

$UseCount = ([regex]::Matches($Js, '\bSTAGE1_PATH_UPGRADE_KEY\b')).Count

if ($DeclCount -ne 1) {
    throw "Validation failed: expected exactly one STAGE1_PATH_UPGRADE_KEY declaration, found $DeclCount."
}
if ($UseCount -lt 2) {
    throw "Validation failed: STAGE1_PATH_UPGRADE_KEY is not referenced after declaration."
}

# Bump visible marker if one of the known questline headers exists.
$KnownHeaders = @(
    "/* Dome Survival - Joseph Cooper GUI v7.4 EXODUS */",
    "/* Dome Survival - Joseph Cooper GUI v7.4.1 INTERACT FIX */"
)

foreach ($Header in $KnownHeaders) {
    if ($Js.Contains($Header)) {
        $Js = $Js.Replace(
            $Header,
            "/* Dome Survival - Joseph Cooper GUI v7.4.2 STAGE1 KEY FIX */"
        )
        break
    }
}

[IO.File]::WriteAllText($Joseph, $Js, $Utf8NoBom)

# Refresh the actual CustomNPCs external-script copies.
$Targets = New-Object System.Collections.Generic.List[string]
$Targets.Add((Join-Path $Root "run\customnpcs\scripts\ecmascript"))

$WorldLocal = Join-Path $Root "run\saves\WASTED_TEST\customnpcs\scripts\ecmascript"
if (Test-Path -LiteralPath (Split-Path -Parent $WorldLocal)) {
    $Targets.Add($WorldLocal)
}

foreach ($TargetDir in $Targets) {
    New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null
    $Target = Join-Path $TargetDir "joseph_cooper_gui.js"
    Copy-Item -LiteralPath $Joseph -Destination $Target -Force

    $SourceHash = (Get-FileHash -LiteralPath $Joseph -Algorithm SHA256).Hash
    $TargetHash = (Get-FileHash -LiteralPath $Target -Algorithm SHA256).Hash

    if ($SourceHash -ne $TargetHash) {
        throw "Script copy verification failed: $Target"
    }

    Write-Host "[OK] Joseph script -> $Target" -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Joseph V7.4.2 - STAGE1 KEY FIX" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] STAGE1_PATH_UPGRADE_KEY restored." -ForegroundColor Green
Write-Host "[OK] Quest progress was NOT reset." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  1. Fully restart Minecraft / RUN_DEV_FULL.bat"
Write-Host "  2. In world: /josephscript apply"
Write-Host "  3. Then: /josephscript inspect"
Write-Host ""
Write-Host "Expected:"
Write-Host "  errored=false"
Write-Host "  valid=true"
exit 0
