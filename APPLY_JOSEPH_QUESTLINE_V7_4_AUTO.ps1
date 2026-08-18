$ErrorActionPreference = "Stop"
$Root = (Resolve-Path $PSScriptRoot).Path
$Joseph = Join-Path $Root "CUSTOMNPCS_STAGE4\joseph_cooper_gui.js"
if (-not (Test-Path -LiteralPath $Joseph)) { throw "Joseph GUI source not found: $Joseph" }

function Get-Version {
    $text = [IO.File]::ReadAllText($Joseph,[Text.Encoding]::UTF8)
    if ($text.Contains("GUI v7.4 EXODUS")) { return "7.4" }
    if ($text.Contains("GUI v7.3.3 RUSSIAN + FULL PATH + REWARDS")) { return "7.3.3" }
    if ($text.Contains("GUI v7.3.2 MULTIMOD + PATH")) { return "7.3.2" }
    if ($text.Contains("GUI v7.3 MULTIMOD + TEST SKIP")) { return "7.3" }
    if ($text.Contains("GUI v7.2 UI + REWARDS FIX")) { return "7.2" }
    if ($text.Contains("GUI v7.1 SURVIVAL QUESTLINE")) { return "7.1" }
    if ($text.Contains("GUI v7.0 RELEASE RESET")) { return "7.0" }
    return "unknown"
}
function Run-Step([string]$file,[string]$label) {
    Write-Host ""
    Write-Host "=== $label ===" -ForegroundColor Cyan
    $path = Join-Path $Root $file
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing patcher: $path" }
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $path
    if ($LASTEXITCODE -ne 0) { throw "$label failed with exit code $LASTEXITCODE" }
}

$version = Get-Version
Write-Host "============================================================"
Write-Host "DomeSurvival Joseph Questline V7.4 AUTO"
Write-Host "============================================================"
Write-Host "Current Joseph version: $version"

if ($version -eq "7.4") { Write-Host "[OK] V7.4 already installed." -ForegroundColor Green; exit 0 }

if ($version -ne "7.3.3") {
    Run-Step "APPLY_JOSEPH_QUESTLINE_V7_3_3_AUTO.ps1" "$version -> V7.3.3"
}
if ((Get-Version) -ne "7.3.3") { throw "Expected V7.3.3 before V7.4, got $(Get-Version)" }

Run-Step "APPLY_JOSEPH_QUESTLINE_V7_4.ps1" "V7.3.3 -> V7.4"
if ((Get-Version) -ne "7.4") { throw "Unexpected final version: $(Get-Version)" }

Write-Host ""
Write-Host "[OK] Joseph Questline V7.4 ready." -ForegroundColor Green
Write-Host "Next: .\dev\RUN_DEV_FULL.bat"
Write-Host "In world: /josephscript apply"
Write-Host "Fast progression: /josephscript nextstage"
exit 0
