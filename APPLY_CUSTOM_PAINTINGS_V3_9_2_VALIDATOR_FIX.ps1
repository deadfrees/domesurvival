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
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_9_2_$Stamp"
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

function Find-PaintingCtor {
    param(
        [string]$Source,
        [string]$Id
    )

    $IdNeedle = '"' + $Id + '"'
    $IdIndex = $Source.IndexOf($IdNeedle, [System.StringComparison]::Ordinal)
    if ($IdIndex -lt 0) {
        throw "PaintingVariant registration id not found: $Id"
    }

    $SecondIdIndex = $Source.IndexOf(
        $IdNeedle,
        $IdIndex + $IdNeedle.Length,
        [System.StringComparison]::Ordinal
    )
    if ($SecondIdIndex -ge 0) {
        throw "PaintingVariant registration id appears more than once: $Id"
    }

    $CtorNeedle = "new PaintingVariant("
    $CtorIndex = $Source.IndexOf(
        $CtorNeedle,
        $IdIndex,
        [System.StringComparison]::Ordinal
    )
    if ($CtorIndex -lt 0) {
        throw "PaintingVariant constructor not found after id: $Id"
    }

    # Make sure we did not accidentally jump into the next registry entry.
    $NextRegister = $Source.IndexOf(
        "PAINTING_VARIANTS.register(",
        $IdIndex + $IdNeedle.Length,
        [System.StringComparison]::Ordinal
    )
    if ($NextRegister -ge 0 -and $CtorIndex -gt $NextRegister) {
        throw "PaintingVariant constructor for $Id was not found inside its own registry entry."
    }

    $ArgsStart = $CtorIndex + $CtorNeedle.Length
    $ArgsEnd = $Source.IndexOf(
        ")",
        $ArgsStart,
        [System.StringComparison]::Ordinal
    )
    if ($ArgsEnd -lt 0) {
        throw "PaintingVariant constructor closing parenthesis not found: $Id"
    }

    $RawArgs = $Source.Substring($ArgsStart, $ArgsEnd - $ArgsStart)
    if ($RawArgs -notmatch '^\s*(\d+)\s*,\s*(\d+)\s*$') {
        throw "Unexpected PaintingVariant constructor arguments for ${Id}: $RawArgs"
    }

    return @{
        ArgsStart = $ArgsStart
        ArgsEnd = $ArgsEnd
        Width = [int]$Matches[1]
        Height = [int]$Matches[2]
    }
}

function Set-PaintingSize {
    param(
        [string]$Source,
        [string]$Id,
        [int]$WidthPixels,
        [int]$HeightPixels
    )

    $Found = Find-PaintingCtor $Source $Id
    $Before = $Source.Substring(0, $Found.ArgsStart)
    $After = $Source.Substring($Found.ArgsEnd)
    return $Before + $WidthPixels + ", " + $HeightPixels + $After
}

function Assert-PaintingSize {
    param(
        [string]$Source,
        [string]$Id,
        [int]$WidthPixels,
        [int]$HeightPixels
    )

    $Found = Find-PaintingCtor $Source $Id
    if ($Found.Width -ne $WidthPixels -or $Found.Height -ne $HeightPixels) {
        throw "V3.9.2 size validation failed for ${Id}: actual $($Found.Width)x$($Found.Height)px, expected ${WidthPixels}x${HeightPixels}px"
    }
}

foreach ($Id in $OneBlock) {
    $Text = Set-PaintingSize $Text $Id 16 16
}

foreach ($Id in $FourBlocks) {
    $Text = Set-PaintingSize $Text $Id 32 32
}

foreach ($Entry in $RestoreV38.GetEnumerator()) {
    $Text = Set-PaintingSize $Text $Entry.Key ([int]$Entry.Value[0]) ([int]$Entry.Value[1])
}

# Validate IN MEMORY before touching the source file.
foreach ($Id in $OneBlock) {
    Assert-PaintingSize $Text $Id 16 16
}
foreach ($Id in $FourBlocks) {
    Assert-PaintingSize $Text $Id 32 32
}
Assert-PaintingSize $Text "06_relaxing_on_grass" 48 64
Assert-PaintingSize $Text "compact_03_airsoft_team" 48 32

# Only now write the Java source.
[IO.File]::WriteAllText($VariantJava, $Text, $Utf8NoBom)

# Reapply the exact V3.9.1 texture payload. Safe after a partially-applied V3.9.1.
Get-ChildItem -LiteralPath $Payload -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($Payload.Length).TrimStart('\','/')
    $destination = Join-Path $Root $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}

# Validate the file after writing as a second guard.
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
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.9.2 - VALIDATOR FIX" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] V3.9.1 regex validator removed." -ForegroundColor Green
Write-Host "[OK] Registry sizes are parsed and checked numerically." -ForegroundColor Green
Write-Host "[OK] 06_relaxing_on_grass = 48x64 px (3x4 blocks)." -ForegroundColor Green
Write-Host "[OK] compact_03_airsoft_team = 48x32 px (3x2 blocks)." -ForegroundColor Green
Write-Host "[OK] Safe over a partially-applied V3.9.1." -ForegroundColor Green
Write-Host "[BUILD] Running full clean build..." -ForegroundColor Cyan

$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_9_2_BUILD_LAST.txt"
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
Write-Host "[OK] Custom Paintings V3.9.2 installed." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "Test:"
Write-Host "  /give @s domesurvival:memory_painting 32"
exit 0
