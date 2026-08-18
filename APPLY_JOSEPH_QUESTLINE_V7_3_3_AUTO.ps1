$ErrorActionPreference = "Stop"
$Root = (Resolve-Path $PSScriptRoot).Path
$Joseph = Join-Path $Root "CUSTOMNPCS_STAGE4\joseph_cooper_gui.js"

if (-not (Test-Path -LiteralPath $Joseph)) {
    throw "Joseph GUI source not found: $Joseph"
}

function Get-JosephVersion {
    param([string]$Path)
    $text = [IO.File]::ReadAllText($Path,[Text.Encoding]::UTF8)
    if ($text.Contains("GUI v7.3.3 RUSSIAN + FULL PATH + REWARDS")) { return "7.3.3" }
    if ($text.Contains("GUI v7.3.2 MULTIMOD + PATH")) { return "7.3.2" }
    if ($text.Contains("GUI v7.3 MULTIMOD + TEST SKIP")) { return "7.3" }
    if ($text.Contains("GUI v7.2 UI + REWARDS FIX")) { return "7.2" }
    if ($text.Contains("GUI v7.1 SURVIVAL QUESTLINE")) { return "7.1" }
    if ($text.Contains("GUI v7.0 RELEASE RESET")) { return "7.0" }
    return "unknown"
}

function Run-Step {
    param([string]$File,[string]$Label)
    $path = Join-Path $Root $File
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing bundled patcher: $path" }
    Write-Host ""
    Write-Host "=== $Label ===" -ForegroundColor Cyan
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $path
    if ($LASTEXITCODE -ne 0) { throw "$Label failed with exit code $LASTEXITCODE" }
}

$version = Get-JosephVersion $Joseph
Write-Host "============================================================"
Write-Host "DomeSurvival Joseph Questline V7.3.3 AUTO"
Write-Host "============================================================"
Write-Host "Current Joseph version: $version"

switch ($version) {
    "7.3.3" {
        Write-Host "[OK] V7.3.3 is already installed." -ForegroundColor Green
        exit 0
    }
    "7.3.2" { }
    "7.3" {
        Run-Step "APPLY_JOSEPH_STAGE1_PATH_V7_3_2.ps1" "V7.3 -> V7.3.2"
    }
    "7.2" {
        Run-Step "APPLY_JOSEPH_QUESTLINE_V7_3.ps1" "V7.2 -> V7.3"
        Run-Step "APPLY_JOSEPH_STAGE1_PATH_V7_3_2.ps1" "V7.3 -> V7.3.2"
    }
    "7.1" {
        Run-Step "APPLY_JOSEPH_QUESTLINE_V7_2.ps1" "V7.1 -> V7.2"
        Run-Step "APPLY_JOSEPH_QUESTLINE_V7_3.ps1" "V7.2 -> V7.3"
        Run-Step "APPLY_JOSEPH_STAGE1_PATH_V7_3_2.ps1" "V7.3 -> V7.3.2"
    }
    "7.0" {
        Run-Step "APPLY_JOSEPH_QUESTLINE_V7_1.ps1" "V7.0 -> V7.1"
        Run-Step "APPLY_JOSEPH_QUESTLINE_V7_2.ps1" "V7.1 -> V7.2"
        Run-Step "APPLY_JOSEPH_QUESTLINE_V7_3.ps1" "V7.2 -> V7.3"
        Run-Step "APPLY_JOSEPH_STAGE1_PATH_V7_3_2.ps1" "V7.3 -> V7.3.2"
    }
    default {
        throw "Unknown Joseph source version. No automatic overwrite was performed."
    }
}

if ((Get-JosephVersion $Joseph) -ne "7.3.2") {
    throw "Expected V7.3.2 before final patch, got: $(Get-JosephVersion $Joseph)"
}

Run-Step "APPLY_JOSEPH_QUESTLINE_V7_3_3.ps1" "V7.3.2 -> V7.3.3"

if ((Get-JosephVersion $Joseph) -ne "7.3.3") {
    throw "Unexpected final version: $(Get-JosephVersion $Joseph)"
}

Write-Host ""
Write-Host "[OK] Joseph Questline V7.3.3 installed." -ForegroundColor Green
Write-Host "Next: .\dev\RUN_DEV_FULL.bat"
Write-Host "In world: /josephscript apply"
exit 0
