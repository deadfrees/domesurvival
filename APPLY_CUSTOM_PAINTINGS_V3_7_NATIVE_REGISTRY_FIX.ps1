$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Payload = Join-Path $PSScriptRoot "PATCH_PAYLOAD"
$Dome = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\DomeSurvival.java"
$RegistryJava = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\registry\ModPaintingVariants.java"
$Gradle = Join-Path $Root "gradlew.bat"

if (-not (Test-Path -LiteralPath $Dome)) {
    throw "DomeSurvival.java not found: $Dome"
}
if (-not (Test-Path -LiteralPath $Gradle)) {
    throw "gradlew.bat not found: $Gradle"
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_7_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Dome -Destination (Join-Path $Backup "DomeSurvival.java") -Force

$OldDataDir = Join-Path $Root "src\main\resources\data\domesurvival\painting_variant"
if (Test-Path -LiteralPath $OldDataDir) {
    Copy-Item -LiteralPath $OldDataDir -Destination (Join-Path $Backup "obsolete_painting_variant_json") -Recurse -Force
}

# Copy Java registry + updated custom tag.
Get-ChildItem -LiteralPath $Payload -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($Payload.Length).TrimStart('\','/')
    $destination = Join-Path $Root $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Text = [IO.File]::ReadAllText($Dome, [Text.Encoding]::UTF8)

$Import = 'import com.wasted.domesurvival.forge.registry.ModPaintingVariants;'
if (-not $Text.Contains($Import)) {
    $Anchor = 'import com.wasted.domesurvival.forge.registry.ModMenuTypes;'
    if ($Text.Contains($Anchor)) {
        $Text = $Text.Replace($Anchor, $Anchor + "`r`n" + $Import)
    } else {
        $PackageAnchor = 'package com.wasted.domesurvival.forge;'
        $Text = $Text.Replace($PackageAnchor, $PackageAnchor + "`r`n`r`n" + $Import)
    }
}

$RegisterLine = '        ModPaintingVariants.PAINTING_VARIANTS.register(modBus);'
if (-not $Text.Contains($RegisterLine.Trim())) {
    $Anchor = '        ModItems.ITEMS.register(modBus);'
    if (-not $Text.Contains($Anchor)) {
        throw "Could not locate ModItems registration anchor in DomeSurvival.java."
    }
    $Text = $Text.Replace($Anchor, $Anchor + "`r`n" + $RegisterLine)
}

if (-not $Text.Contains($Import) -or -not $Text.Contains($RegisterLine.Trim())) {
    throw "DomeSurvival.java validation failed after native painting registry injection."
}

[IO.File]::WriteAllText($Dome, $Text, $Utf8NoBom)

# 1.20.1 does not consume these JSON painting definitions. Remove them from
# the active source tree so a later developer does not mistake them for the
# authoritative registry source. A backup was already made above.
if (Test-Path -LiteralPath $OldDataDir) {
    Remove-Item -LiteralPath $OldDataDir -Recurse -Force
}

# Structural validation before compiling.
$RegistryText = [IO.File]::ReadAllText($RegistryJava, [Text.Encoding]::UTF8)
$NativeCount = ([regex]::Matches($RegistryText, 'PAINTING_VARIANTS\.register\(')).Count

if ($NativeCount -ne 44) {
    throw "Expected 44 native PaintingVariant registrations, found $NativeCount."
}

$TextureDir = Join-Path $Root "src\main\resources\assets\domesurvival\textures\painting"
$OriginalTextures = (Get-ChildItem -LiteralPath $TextureDir -Filter "*.png" | Where-Object { $_.Name -notlike "compact_*" } | Measure-Object).Count
$CompactTextures = (Get-ChildItem -LiteralPath $TextureDir -Filter "compact_*.png" | Measure-Object).Count

if ($OriginalTextures -lt 22 -or $CompactTextures -lt 22) {
    throw "Painting texture validation failed. original=$OriginalTextures compact=$CompactTextures"
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.7 - NATIVE REGISTRY FIX" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] 44 PaintingVariant objects registered natively for Forge 1.20.1." -ForegroundColor Green
Write-Host "[OK] Obsolete painting_variant JSON definitions removed from active source." -ForegroundColor Green
Write-Host "[OK] Custom memory_paintings tag updated to 44 native registry IDs." -ForegroundColor Green
Write-Host "[BUILD] Running full clean build..." -ForegroundColor Cyan

$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_7_BUILD_LAST.txt"
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
Write-Host "[OK] Custom Paintings V3.7 native registry fix installed." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "Then test:"
Write-Host "  /give @s domesurvival:memory_painting"
exit 0
