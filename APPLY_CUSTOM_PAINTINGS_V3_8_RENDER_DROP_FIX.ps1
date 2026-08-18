$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Payload = Join-Path $PSScriptRoot "PATCH_PAYLOAD"
$Dome = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\DomeSurvival.java"
$Gradle = Join-Path $Root "gradlew.bat"

if (-not (Test-Path -LiteralPath $Dome)) {
    throw "DomeSurvival.java not found: $Dome"
}
if (-not (Test-Path -LiteralPath $Gradle)) {
    throw "gradlew.bat not found: $Gradle"
}

$TextureDir = Join-Path $Root "src\main\resources\assets\domesurvival\textures\painting"
if (-not (Test-Path -LiteralPath $TextureDir)) {
    throw "Painting texture directory is missing. Install V3.6/V3.7 first."
}

$OriginalTextures = (
    Get-ChildItem -LiteralPath $TextureDir -Filter "*.png" |
    Where-Object { $_.Name -notlike "compact_*" } |
    Measure-Object
).Count
$CompactTextures = (
    Get-ChildItem -LiteralPath $TextureDir -Filter "compact_*.png" |
    Measure-Object
).Count

if ($OriginalTextures -lt 22 -or $CompactTextures -lt 22) {
    throw "Expected 22 original + 22 compact textures. original=$OriginalTextures compact=$CompactTextures"
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_8_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Dome -Destination (Join-Path $Backup "DomeSurvival.java") -Force

$ExistingFiles = @(
    "src\main\java\com\wasted\domesurvival\forge\registry\ModPaintingVariants.java",
    "src\main\java\com\wasted\domesurvival\forge\item\MemoryPaintingItem.java"
)
foreach ($Rel in $ExistingFiles) {
    $Existing = Join-Path $Root $Rel
    if (Test-Path -LiteralPath $Existing) {
        $SafeName = ($Rel -replace '[\\/:*?"<>|]', '_')
        Copy-Item -LiteralPath $Existing -Destination (Join-Path $Backup $SafeName) -Force
    }
}

# The previous manual atlas override is not needed in 1.20.1.
# Vanilla's paintings atlas already scans textures/painting across namespaces.
$ManualAtlas = Join-Path $Root "src\main\resources\assets\minecraft\atlases\paintings.json"
if (Test-Path -LiteralPath $ManualAtlas) {
    Copy-Item -LiteralPath $ManualAtlas -Destination (Join-Path $Backup "manual_paintings_atlas.json") -Force
    Remove-Item -LiteralPath $ManualAtlas -Force
    Write-Host "[OK] Removed unnecessary manual paintings atlas override (backup kept)." -ForegroundColor Green
}

Get-ChildItem -LiteralPath $Payload -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($Payload.Length).TrimStart('\','/')
    $destination = Join-Path $Root $relative
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Text = [IO.File]::ReadAllText($Dome, [Text.Encoding]::UTF8)

$Import = 'import com.wasted.domesurvival.forge.registry.ModEntityTypes;'
if (-not $Text.Contains($Import)) {
    $Anchor = 'import com.wasted.domesurvival.forge.registry.ModMenuTypes;'
    if ($Text.Contains($Anchor)) {
        $Text = $Text.Replace($Anchor, $Anchor + "`r`n" + $Import)
    } else {
        throw "Could not locate ModMenuTypes import anchor."
    }
}

$Register = '        ModEntityTypes.ENTITY_TYPES.register(modBus);'
if (-not $Text.Contains($Register.Trim())) {
    $Anchor = '        ModItems.ITEMS.register(modBus);'
    if (-not $Text.Contains($Anchor)) {
        throw "Could not locate ModItems event-bus registration anchor."
    }
    $Text = $Text.Replace($Anchor, $Anchor + "`r`n" + $Register)
}

[IO.File]::WriteAllText($Dome, $Text, $Utf8NoBom)

# Structural checks: all PaintingVariant dimensions must be >= 16 and divisible by 16.
$VariantFile = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\registry\ModPaintingVariants.java"
$VariantText = [IO.File]::ReadAllText($VariantFile, [Text.Encoding]::UTF8)
$Matches = [regex]::Matches($VariantText, 'new PaintingVariant\((\d+),\s*(\d+)\)')
if ($Matches.Count -ne 44) {
    throw "Expected 44 PaintingVariant registrations, found $($Matches.Count)."
}
foreach ($M in $Matches) {
    $W = [int]$M.Groups[1].Value
    $H = [int]$M.Groups[2].Value
    if ($W -lt 16 -or $H -lt 16 -or ($W % 16) -ne 0 -or ($H % 16) -ne 0) {
        throw "Invalid 1.20.1 PaintingVariant pixel size: ${W}x${H}"
    }
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.8 - RENDER + DROP FIX" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] All 44 variants now use 1.20.1 pixel dimensions (16 px per block)." -ForegroundColor Green
Write-Host "[OK] Memory paintings now use their own persistent entity type." -ForegroundColor Green
Write-Host "[OK] Breaking/support loss drops domesurvival:memory_painting." -ForegroundColor Green
Write-Host "[BUILD] Running full clean build..." -ForegroundColor Cyan

$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_8_BUILD_LAST.txt"
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
Write-Host "[OK] Custom Paintings V3.8 installed." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "Test:"
Write-Host "  /give @s domesurvival:memory_painting"
Write-Host "  Place it, verify the image, then break the support block."
exit 0
