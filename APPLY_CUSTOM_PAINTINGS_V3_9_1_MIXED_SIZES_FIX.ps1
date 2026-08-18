$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Payload = Join-Path $PSScriptRoot "PATCH_PAYLOAD"
$VariantJava = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\registry\ModPaintingVariants.java"
$TextureDir = Join-Path $Root "src\main\resources\assets\domesurvival\textures\painting"
$Gradle = Join-Path $Root "gradlew.bat"

if (-not (Test-Path -LiteralPath $VariantJava)) {
    throw "ModPaintingVariants.java not found. Install working Custom Paintings V3.8 first."
}
if (-not (Test-Path -LiteralPath $TextureDir)) {
    throw "Painting texture directory not found: $TextureDir"
}
if (-not (Test-Path -LiteralPath $Gradle)) {
    throw "gradlew.bat not found: $Gradle"
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_9_1_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $VariantJava -Destination (Join-Path $Backup "ModPaintingVariants.java") -Force

$Changed = @(
    "07_pink_hat_portrait",
    "09_white_hat_portrait",
    "10_flexing_portrait",
    "12_kitchen_character",
    "16_tricolor_portrait",
    "17_bee_hero_amber_hive",
    "19_night_selfie_friendship",
    "01_trio_friends",
    "03_airsoft_team",
    "06_relaxing_on_grass",
    "13_music_studio_friends",
    "15_voxel_company_bright_light",
    "18_wedding_kiss_tree",
    "22_party_toast_indoor"
)

foreach ($Id in $Changed) {
    $Texture = Join-Path $TextureDir ($Id + ".png")
    if (Test-Path -LiteralPath $Texture) {
        Copy-Item -LiteralPath $Texture -Destination (Join-Path $Backup ($Id + ".png")) -Force
    }
}

# V3.9 could stop after partially editing the Java file. V3.9.1 therefore
# does not try to patch that partially modified source. It replaces the whole
# registry definition with a known-good V3.8-compatible file.
Get-ChildItem -LiteralPath $Payload -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($Payload.Length).TrimStart('\','/')
    $destination = Join-Path $Root $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}

$Updated = [IO.File]::ReadAllText($VariantJava, [Text.Encoding]::UTF8)

$OneBlock = @(
    "07_pink_hat_portrait",
    "09_white_hat_portrait",
    "10_flexing_portrait",
    "12_kitchen_character",
    "16_tricolor_portrait",
    "17_bee_hero_amber_hive",
    "19_night_selfie_friendship"
)
$FourBlocks = @(
    "01_trio_friends",
    "03_airsoft_team",
    "06_relaxing_on_grass",
    "13_music_studio_friends",
    "15_voxel_company_bright_light",
    "18_wedding_kiss_tree",
    "22_party_toast_indoor"
)

foreach ($Id in $OneBlock) {
    $Pattern = 'PAINTING_VARIANTS\.register\("' + [regex]::Escape($Id) + '",\s*\(\)\s*->\s*new PaintingVariant\(16,\s*16\)\)'
    if ([regex]::Matches($Updated, $Pattern).Count -ne 1) {
        throw "V3.9.1 validation failed for 1x1 painting: $Id"
    }
}
foreach ($Id in $FourBlocks) {
    $Pattern = 'PAINTING_VARIANTS\.register\("' + [regex]::Escape($Id) + '",\s*\(\)\s*->\s*new PaintingVariant\(32,\s*32\)\)'
    if ([regex]::Matches($Updated, $Pattern).Count -ne 1) {
        throw "V3.9.1 validation failed for 2x2 painting: $Id"
    }
}

$AllRegistrations = [regex]::Matches($Updated, 'PAINTING_VARIANTS\.register\(').Count
if ($AllRegistrations -ne 44) {
    throw "Expected 44 painting registrations after V3.9.1, found $AllRegistrations."
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.9.1 - MIXED SIZES FIX" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] Recovered safely from partially applied V3.9." -ForegroundColor Green
Write-Host "[OK] 7 paintings = 1x1 block." -ForegroundColor Green
Write-Host "[OK] 7 paintings = 2x2 blocks (4 blocks total)." -ForegroundColor Green
Write-Host "[OK] 8 paintings keep their original large V3.8 size." -ForegroundColor Green
Write-Host "[OK] 44 total variants preserved." -ForegroundColor Green
Write-Host "[BUILD] Running full clean build..." -ForegroundColor Cyan

$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_9_1_BUILD_LAST.txt"
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
Write-Host "[OK] Custom Paintings V3.9.1 installed." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "Test:"
Write-Host "  /give @s domesurvival:memory_painting 32"
exit 0
