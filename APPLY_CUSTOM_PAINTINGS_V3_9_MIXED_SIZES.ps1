$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Payload = Join-Path $PSScriptRoot "PATCH_PAYLOAD"
$VariantJava = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\registry\ModPaintingVariants.java"
$TextureDir = Join-Path $Root "src\main\resources\assets\domesurvival\textures\painting"
$Gradle = Join-Path $Root "gradlew.bat"

if (-not (Test-Path -LiteralPath $VariantJava)) {
    throw "ModPaintingVariants.java not found. Install Custom Paintings V3.8 first."
}
if (-not (Test-Path -LiteralPath $TextureDir)) {
    throw "Painting texture directory not found: $TextureDir"
}
if (-not (Test-Path -LiteralPath $Gradle)) {
    throw "gradlew.bat not found: $Gradle"
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_9_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $VariantJava -Destination (Join-Path $Backup "ModPaintingVariants.java") -Force

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

$Unchanged = @(
    "02_recording_in_yard",
    "04_fishing_closeup",
    "05_calm_lake_fishing",
    "08_watermelon_park",
    "11_prize_shop_winners",
    "14_mirror_group_selfie",
    "20_brown_suit_limo",
    "21_bw_party_point"
)

# Backup only the textures that V3.9 intentionally changes.
$ChangedIds = @($OneBlock + $FourBlocks)
foreach ($Id in $ChangedIds) {
    $Current = Join-Path $TextureDir ($Id + ".png")
    if (-not (Test-Path -LiteralPath $Current)) {
        throw "Painting texture missing before V3.9: $Current"
    }
    Copy-Item -LiteralPath $Current -Destination (Join-Path $Backup ($Id + ".png")) -Force
}

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Text = [IO.File]::ReadAllText($VariantJava, [Text.Encoding]::UTF8)

function Set-PaintingSize {
    param(
        [string]$Source,
        [string]$Id,
        [int]$WidthPixels,
        [int]$HeightPixels
    )

    $EscapedId = [regex]::Escape($Id)
    $Pattern = 'PAINTING_VARIANTS\.register\("' + $EscapedId + '",\s*\(\)\s*->\s*new PaintingVariant\(\d+,\s*\d+\)\)'
    $Replacement = 'PAINTING_VARIANTS.register("' + $Id + '", () -> new PaintingVariant(' + $WidthPixels + ', ' + $HeightPixels + '))'

    $Matches = [regex]::Matches($Source, $Pattern)
    if ($Matches.Count -ne 1) {
        throw "Expected exactly one PaintingVariant registration for $Id, found $($Matches.Count)."
    }

    return [regex]::Replace($Source, $Pattern, $Replacement, 1)
}

# 1 block total = 1x1 blocks = 16x16 px on Minecraft 1.20.1.
foreach ($Id in $OneBlock) {
    $Text = Set-PaintingSize $Text $Id 16 16
}

# 4 blocks total = 2x2 blocks = 32x32 px.
foreach ($Id in $FourBlocks) {
    $Text = Set-PaintingSize $Text $Id 32 32
}

[IO.File]::WriteAllText($VariantJava, $Text, $Utf8NoBom)

# Copy the square-cropped textures that match the new aspect ratios.
Get-ChildItem -LiteralPath $Payload -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($Payload.Length).TrimStart('\','/')
    $destination = Join-Path $Root $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}

# Validate exact V3.9 dimensions.
$Updated = [IO.File]::ReadAllText($VariantJava, [Text.Encoding]::UTF8)
foreach ($Id in $OneBlock) {
    if (-not $Updated.Contains('register("' + $Id + '", () -> new PaintingVariant(16, 16))')) {
        throw "V3.9 validation failed for 1x1 painting: $Id"
    }
}
foreach ($Id in $FourBlocks) {
    if (-not $Updated.Contains('register("' + $Id + '", () -> new PaintingVariant(32, 32))')) {
        throw "V3.9 validation failed for 2x2 painting: $Id"
    }
}

# The 8 unchanged variants must still exist, but V3.9 deliberately leaves
# their current V3.8 dimensions intact.
foreach ($Id in $Unchanged) {
    if (-not $Updated.Contains('register("' + $Id + '"')) {
        throw "Unchanged painting registration disappeared: $Id"
    }
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.9 - MIXED SIZES" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] 7 paintings set to 1x1 block." -ForegroundColor Green
Write-Host "[OK] 7 paintings set to 2x2 blocks (4 blocks total)." -ForegroundColor Green
Write-Host "[OK] 8 paintings kept at their original large sizes." -ForegroundColor Green
Write-Host "[OK] Changed paintings received square crops from the same prepared images." -ForegroundColor Green
Write-Host "[BUILD] Running full clean build..." -ForegroundColor Cyan

$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_9_BUILD_LAST.txt"
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
Write-Host "[OK] Custom Paintings V3.9 installed." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "Test:"
Write-Host "  /give @s domesurvival:memory_painting 32"
exit 0
