$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Target = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\item\MemoryPaintingItem.java"
$Payload = Join-Path $PSScriptRoot "PATCH_PAYLOAD\MemoryPaintingItem.java"
$Gradle = Join-Path $Root "gradlew.bat"

if (-not (Test-Path -LiteralPath $Target)) {
    throw "MemoryPaintingItem.java not found. Install V3.x first: $Target"
}
if (-not (Test-Path -LiteralPath $Payload)) {
    throw "Patch payload missing: $Payload"
}
if (-not (Test-Path -LiteralPath $Gradle)) {
    throw "gradlew.bat not found: $Gradle"
}

$RequiredVariant = Join-Path $Root "src\main\resources\data\domesurvival\painting_variant\01_trio_friends.json"
$RequiredTexture = Join-Path $Root "src\main\resources\assets\domesurvival\textures\painting\01_trio_friends.png"

if (-not (Test-Path -LiteralPath $RequiredVariant)) {
    throw "Painting variant resources are missing. Reinstall Custom Paintings V3.1 first."
}
if (-not (Test-Path -LiteralPath $RequiredTexture)) {
    throw "Painting textures are missing. Reinstall Custom Paintings V3.1 first."
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_4_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Target -Destination (Join-Path $Backup "MemoryPaintingItem.java") -Force

Copy-Item -LiteralPath $Payload -Destination $Target -Force

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.4 - PLACEMENT FIX" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] Removed runtime dependency on the custom painting tag." -ForegroundColor Green
Write-Host "[OK] Custom variants are resolved directly by registry ID." -ForegroundColor Green
Write-Host "[OK] Added nearby wall-anchor search for large paintings." -ForegroundColor Green
Write-Host "[BUILD] Running compileJava + processResources..." -ForegroundColor Cyan

$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_4_BUILD_LAST.txt"
$BuildLogName = Split-Path -Leaf $BuildLog

Push-Location $Root
try {
    & cmd.exe /d /c "gradlew.bat compileJava processResources --no-daemon > `"$BuildLogName`" 2>&1"
    $Exit = $LASTEXITCODE
}
finally {
    Pop-Location
}

if ($Exit -ne 0) {
    Write-Host "[ERROR] Build failed." -ForegroundColor Red
    Write-Host "Log: $BuildLog" -ForegroundColor Red
    Write-Host "Backup: $Backup" -ForegroundColor Yellow
    exit $Exit
}

Write-Host ""
Write-Host "[OK] compileJava + processResources succeeded." -ForegroundColor Green
Write-Host "[OK] Custom Paintings V3.4 installed." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "Test on a flat solid wall at least 5 blocks high and 4 blocks wide:"
Write-Host "  /give @s domesurvival:memory_painting"
exit 0
