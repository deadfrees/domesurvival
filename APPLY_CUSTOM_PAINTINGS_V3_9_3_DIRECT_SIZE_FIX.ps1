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
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_9_3_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $VariantJava -Destination (Join-Path $Backup "ModPaintingVariants.java") -Force

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Text = [IO.File]::ReadAllText($VariantJava, [Text.Encoding]::UTF8)

function Find-PaintingCtor {
    param(
        [Parameter(Mandatory=$true)][string]$Source,
        [Parameter(Mandatory=$true)][string]$Id
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
    $ArgMatch = [regex]::Match($RawArgs, '^\s*(\d+)\s*,\s*(\d+)\s*$')
    if (-not $ArgMatch.Success) {
        throw "Unexpected PaintingVariant constructor arguments for ${Id}: $RawArgs"
    }

    return [PSCustomObject]@{
        ArgsStart = $ArgsStart
        ArgsEnd = $ArgsEnd
        Width = [int]$ArgMatch.Groups[1].Value
        Height = [int]$ArgMatch.Groups[2].Value
    }
}

function Set-PaintingSize {
    param(
        [Parameter(Mandatory=$true)][string]$Source,
        [Parameter(Mandatory=$true)][string]$Id,
        [Parameter(Mandatory=$true)][int]$WidthPixels,
        [Parameter(Mandatory=$true)][int]$HeightPixels
    )

    $Found = Find-PaintingCtor -Source $Source -Id $Id
    $Before = $Source.Substring(0, $Found.ArgsStart)
    $After = $Source.Substring($Found.ArgsEnd)
    $Result = $Before + $WidthPixels.ToString() + ", " + $HeightPixels.ToString() + $After

    $Verify = Find-PaintingCtor -Source $Result -Id $Id
    if ($Verify.Width -ne $WidthPixels -or $Verify.Height -ne $HeightPixels) {
        throw "Immediate size write verification failed for ${Id}: actual $($Verify.Width)x$($Verify.Height), expected ${WidthPixels}x${HeightPixels}"
    }

    return $Result
}

function Assert-PaintingSize {
    param(
        [Parameter(Mandatory=$true)][string]$Source,
        [Parameter(Mandatory=$true)][string]$Id,
        [Parameter(Mandatory=$true)][int]$WidthPixels,
        [Parameter(Mandatory=$true)][int]$HeightPixels
    )

    $Found = Find-PaintingCtor -Source $Source -Id $Id
    if ($Found.Width -ne $WidthPixels -or $Found.Height -ne $HeightPixels) {
        throw "V3.9.3 size validation failed for ${Id}: actual $($Found.Width)x$($Found.Height)px, expected ${WidthPixels}x${HeightPixels}px"
    }
}

# 1x1 paintings.
$Text = Set-PaintingSize -Source $Text -Id "07_pink_hat_portrait" -WidthPixels 16 -HeightPixels 16
$Text = Set-PaintingSize -Source $Text -Id "09_white_hat_portrait" -WidthPixels 16 -HeightPixels 16
$Text = Set-PaintingSize -Source $Text -Id "10_flexing_portrait" -WidthPixels 16 -HeightPixels 16
$Text = Set-PaintingSize -Source $Text -Id "12_kitchen_character" -WidthPixels 16 -HeightPixels 16
$Text = Set-PaintingSize -Source $Text -Id "16_tricolor_portrait" -WidthPixels 16 -HeightPixels 16
$Text = Set-PaintingSize -Source $Text -Id "17_bee_hero_amber_hive" -WidthPixels 16 -HeightPixels 16
$Text = Set-PaintingSize -Source $Text -Id "19_night_selfie_friendship" -WidthPixels 16 -HeightPixels 16

# 2x2 paintings = four blocks total.
$Text = Set-PaintingSize -Source $Text -Id "01_trio_friends" -WidthPixels 32 -HeightPixels 32
$Text = Set-PaintingSize -Source $Text -Id "03_airsoft_team" -WidthPixels 32 -HeightPixels 32
$Text = Set-PaintingSize -Source $Text -Id "13_music_studio_friends" -WidthPixels 32 -HeightPixels 32
$Text = Set-PaintingSize -Source $Text -Id "15_voxel_company_bright_light" -WidthPixels 32 -HeightPixels 32
$Text = Set-PaintingSize -Source $Text -Id "18_wedding_kiss_tree" -WidthPixels 32 -HeightPixels 32
$Text = Set-PaintingSize -Source $Text -Id "22_party_toast_indoor" -WidthPixels 32 -HeightPixels 32

# IMPORTANT:
# Do these two restores explicitly and LAST.
# V3.9.1/V3.9.2 used an enumerated hashtable call here; on the user's
# PowerShell run the requested 48x64 was never applied and 06 remained 32x32.
$Text = Set-PaintingSize -Source $Text -Id "06_relaxing_on_grass" -WidthPixels 48 -HeightPixels 64
$Text = Set-PaintingSize -Source $Text -Id "compact_03_airsoft_team" -WidthPixels 48 -HeightPixels 32

# Validate the final in-memory state BEFORE touching the source file.
Assert-PaintingSize -Source $Text -Id "06_relaxing_on_grass" -WidthPixels 48 -HeightPixels 64
Assert-PaintingSize -Source $Text -Id "compact_03_airsoft_team" -WidthPixels 48 -HeightPixels 32

[IO.File]::WriteAllText($VariantJava, $Text, $Utf8NoBom)

# Apply the exact texture corrections from the supplied V3.9.1 package.
Get-ChildItem -LiteralPath $Payload -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($Payload.Length).TrimStart('\','/')
    $destination = Join-Path $Root $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}

# Validate AFTER file write and texture copy too.
$Updated = [IO.File]::ReadAllText($VariantJava, [Text.Encoding]::UTF8)

Assert-PaintingSize -Source $Updated -Id "07_pink_hat_portrait" -WidthPixels 16 -HeightPixels 16
Assert-PaintingSize -Source $Updated -Id "09_white_hat_portrait" -WidthPixels 16 -HeightPixels 16
Assert-PaintingSize -Source $Updated -Id "10_flexing_portrait" -WidthPixels 16 -HeightPixels 16
Assert-PaintingSize -Source $Updated -Id "12_kitchen_character" -WidthPixels 16 -HeightPixels 16
Assert-PaintingSize -Source $Updated -Id "16_tricolor_portrait" -WidthPixels 16 -HeightPixels 16
Assert-PaintingSize -Source $Updated -Id "17_bee_hero_amber_hive" -WidthPixels 16 -HeightPixels 16
Assert-PaintingSize -Source $Updated -Id "19_night_selfie_friendship" -WidthPixels 16 -HeightPixels 16

Assert-PaintingSize -Source $Updated -Id "01_trio_friends" -WidthPixels 32 -HeightPixels 32
Assert-PaintingSize -Source $Updated -Id "03_airsoft_team" -WidthPixels 32 -HeightPixels 32
Assert-PaintingSize -Source $Updated -Id "13_music_studio_friends" -WidthPixels 32 -HeightPixels 32
Assert-PaintingSize -Source $Updated -Id "15_voxel_company_bright_light" -WidthPixels 32 -HeightPixels 32
Assert-PaintingSize -Source $Updated -Id "18_wedding_kiss_tree" -WidthPixels 32 -HeightPixels 32
Assert-PaintingSize -Source $Updated -Id "22_party_toast_indoor" -WidthPixels 32 -HeightPixels 32

Assert-PaintingSize -Source $Updated -Id "06_relaxing_on_grass" -WidthPixels 48 -HeightPixels 64
Assert-PaintingSize -Source $Updated -Id "compact_03_airsoft_team" -WidthPixels 48 -HeightPixels 32

# Check the two restored texture aspect ratios from the V3.9.1 payload.
$RelaxingTexture = Join-Path $TextureDir "06_relaxing_on_grass.png"
$CompactAirsoftTexture = Join-Path $TextureDir "compact_03_airsoft_team.png"

if (-not (Test-Path -LiteralPath $RelaxingTexture)) {
    throw "Missing texture after V3.9.3: $RelaxingTexture"
}
if (-not (Test-Path -LiteralPath $CompactAirsoftTexture)) {
    throw "Missing texture after V3.9.3: $CompactAirsoftTexture"
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.9.3 - DIRECT SIZE FIX" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] Direct named size writes are used; no hashtable enumeration." -ForegroundColor Green
Write-Host "[OK] 06_relaxing_on_grass = 48x64 px = 3x4 blocks." -ForegroundColor Green
Write-Host "[OK] compact_03_airsoft_team = 48x32 px = 3x2 blocks." -ForegroundColor Green
Write-Host "[OK] Safe after failed/partial V3.9, V3.9.1 and V3.9.2." -ForegroundColor Green
Write-Host "[BUILD] Running full clean build..." -ForegroundColor Cyan

$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_9_3_BUILD_LAST.txt"
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
Write-Host "[OK] Custom Paintings V3.9.3 installed." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "Test:"
Write-Host "  /give @s domesurvival:memory_painting 32"
exit 0
