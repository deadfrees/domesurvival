$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Payload = Join-Path $PSScriptRoot "PATCH_PAYLOAD"
$VariantJava = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\registry\ModPaintingVariants.java"
$TextureDir = Join-Path $Root "src\main\resources\assets\domesurvival\textures\painting"
$Gradle = Join-Path $Root "gradlew.bat"

if (-not (Test-Path -LiteralPath $VariantJava)) {
    throw "ModPaintingVariants.java not found. Install the working Custom Paintings V3.8 first."
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
    "13_music_studio_friends",
    "15_voxel_company_bright_light",
    "18_wedding_kiss_tree",
    "22_party_toast_indoor"
)

# User explicitly asked to return these to the V3.8 behavior/sizes.
$RestoreV38 = @{
    "06_relaxing_on_grass" = @(48, 64)
    "compact_03_airsoft_team" = @(48, 32)
}

$ChangedIds = New-Object System.Collections.Generic.List[string]
foreach ($Id in $OneBlock) { $ChangedIds.Add($Id) }
foreach ($Id in $FourBlocks) { $ChangedIds.Add($Id) }
foreach ($Id in $RestoreV38.Keys) { $ChangedIds.Add($Id) }

foreach ($Id in $ChangedIds) {
    $Current = Join-Path $TextureDir ($Id + ".png")
    if (Test-Path -LiteralPath $Current) {
        Copy-Item -LiteralPath $Current -Destination (Join-Path $Backup ($Id + ".png")) -Force
    }
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

    # V3.9 failed because its validation depended on exact formatting.
    # V3.9.1 accepts both multiline V3.8 registrations and partially-written
    # single-line V3.9 registrations, so it is safe after the failed attempt.
    $EscapedId = [regex]::Escape($Id)
    $Pattern =
        '(PAINTING_VARIANTS\.register\(\s*"' + $EscapedId +
        '"\s*,\s*\(\)\s*->\s*new PaintingVariant\()\s*\d+\s*,\s*\d+\s*(\)\s*\))'

    $Matches = [regex]::Matches(
        $Source,
        $Pattern,
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )

    if ($Matches.Count -ne 1) {
        throw "Expected exactly one PaintingVariant registration for $Id, found $($Matches.Count)."
    }

    $Replacement = '${1}' + $WidthPixels + ', ' + $HeightPixels + '${2}'
    return [regex]::Replace(
        $Source,
        $Pattern,
        $Replacement,
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )
}

function Assert-PaintingSize {
    param(
        [string]$Source,
        [string]$Id,
        [int]$WidthPixels,
        [int]$HeightPixels
    )

    $EscapedId = [regex]::Escape($Id)
    $Pattern =
        'PAINTING_VARIANTS\.register\(\s*"' + $EscapedId +
        '"\s*,\s*\(\)\s*->\s*new PaintingVariant\(\s*' +
        $WidthPixels + '\s*,\s*' + $HeightPixels + '\s*\)\s*\)'

    if (-not [regex]::IsMatch(
        $Source,
        $Pattern,
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )) {
        throw "V3.9.1 size validation failed for ${Id}: expected ${WidthPixels}x${HeightPixels}px"
    }
}

foreach ($Id in $OneBlock) {
    $Text = Set-PaintingSize $Text $Id 16 16
}

foreach ($Id in $FourBlocks) {
    $Text = Set-PaintingSize $Text $Id 32 32
}

foreach ($Entry in $RestoreV38.GetEnumerator()) {
    $Text = Set-PaintingSize $Text $Entry.Key $Entry.Value[0] $Entry.Value[1]
}

[IO.File]::WriteAllText($VariantJava, $Text, $Utf8NoBom)

# Restore/apply textures after registry dimensions are fixed.
Get-ChildItem -LiteralPath $Payload -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($Payload.Length).TrimStart('\','/')
    $destination = Join-Path $Root $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}

$Updated = [IO.File]::ReadAllText($VariantJava, [Text.Encoding]::UTF8)

foreach ($Id in $OneBlock) {
    Assert-PaintingSize $Updated $Id 16 16
}
foreach ($Id in $FourBlocks) {
    Assert-PaintingSize $Updated $Id 32 32
}
Assert-PaintingSize $Updated "06_relaxing_on_grass" 48 64
Assert-PaintingSize $Updated "compact_03_airsoft_team" 48 32

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.9.1 - MIXED SIZES HOTFIX" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] Installer now tolerates V3.8 and partially-applied V3.9 source formatting." -ForegroundColor Green
Write-Host "[OK] 7 paintings -> 1x1 block." -ForegroundColor Green
Write-Host "[OK] 6 paintings -> 2x2 blocks (4 blocks total)." -ForegroundColor Green
Write-Host "[OK] 06_relaxing_on_grass restored to V3.8 size: 3x4 blocks." -ForegroundColor Green
Write-Host "[OK] compact_03_airsoft_team enforced at V3.8 size: 3x2 blocks." -ForegroundColor Green
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
