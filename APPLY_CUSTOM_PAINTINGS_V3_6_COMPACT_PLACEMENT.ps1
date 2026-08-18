$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Payload = Join-Path $PSScriptRoot "PATCH_PAYLOAD"
$Target = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\item\MemoryPaintingItem.java"
$Gradle = Join-Path $Root "gradlew.bat"

if (-not (Test-Path -LiteralPath $Target)) {
    throw "MemoryPaintingItem.java not found. Install Custom Paintings V3.x first."
}
if (-not (Test-Path -LiteralPath $Gradle)) {
    throw "gradlew.bat not found: $Gradle"
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_6_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Target -Destination (Join-Path $Backup "MemoryPaintingItem.java") -Force

Get-ChildItem -LiteralPath $Payload -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($Payload.Length).TrimStart('\','/')
    $destination = Join-Path $Root $relative
    $destinationDir = Split-Path -Parent $destination
    New-Item -ItemType Directory -Force -Path $destinationDir | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}

$CompactVariantCount = (
    Get-ChildItem -LiteralPath (Join-Path $Root "src\main\resources\data\domesurvival\painting_variant") `
        -Filter "compact_*.json" |
    Measure-Object
).Count

$CompactTextureCount = (
    Get-ChildItem -LiteralPath (Join-Path $Root "src\main\resources\assets\domesurvival\textures\painting") `
        -Filter "compact_*.png" |
    Measure-Object
).Count

if ($CompactVariantCount -lt 22 -or $CompactTextureCount -lt 22) {
    throw "Compact painting resource validation failed. variants=$CompactVariantCount textures=$CompactTextureCount"
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.6 - COMPACT PLACEMENT" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] 22 compact fallback variants installed." -ForegroundColor Green
Write-Host "[OK] Large variants remain preferred on large walls." -ForegroundColor Green
Write-Host "[BUILD] Running full clean build..." -ForegroundColor Cyan

$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_6_BUILD_LAST.txt"
$BuildLogName = Split-Path -Leaf $BuildLog
Push-Location $Root
try {
    & cmd.exe /d /c "gradlew.bat -PdomeFullDev=true clean build --no-daemon > `"$BuildLogName`" 2>&1"
    $Exit = $LASTEXITCODE
}
finally {
    Pop-Location
}

if ($Exit -ne 0) {
    Write-Host "[ERROR] Full build failed." -ForegroundColor Red
    Write-Host "Log: $BuildLog" -ForegroundColor Red
    Write-Host "Backup: $Backup" -ForegroundColor Yellow
    exit $Exit
}

Write-Host ""
Write-Host "[OK] Full clean build succeeded." -ForegroundColor Green
Write-Host "[OK] Custom Paintings V3.6 installed." -ForegroundColor Green
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "Test:"
Write-Host "  /give @s domesurvival:memory_painting"
Write-Host "  Place it on a flat wall at least 2 blocks wide and 3 blocks high."
exit 0
