$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Target = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\item\MemoryPaintingItem.java"
$Gradle = Join-Path $Root "gradlew.bat"

if (-not (Test-Path -LiteralPath $Target)) {
    throw "MemoryPaintingItem.java not found: $Target"
}
if (-not (Test-Path -LiteralPath $Gradle)) {
    throw "gradlew.bat not found: $Gradle"
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_3_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Target -Destination (Join-Path $Backup "MemoryPaintingItem.java") -Force

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Text = [IO.File]::ReadAllText($Target, [Text.Encoding]::UTF8)

# PaintingVariantTags.create(...) is private in the user's exact 1.20.1 mappings.
# Use the public vanilla TagKey factory for the PAINTING_VARIANT registry.
$Old1 = 'PaintingVariantTags.create(DomeSurvival.MOD_ID + ":memory_paintings");'
$Old2 = 'PaintingVariantTags.create(new ResourceLocation(DomeSurvival.MOD_ID, "memory_paintings"));'
$New = 'TagKey.create(Registries.PAINTING_VARIANT, new ResourceLocation(DomeSurvival.MOD_ID, "memory_paintings"));'

if ($Text.Contains($Old1)) {
    $Text = $Text.Replace($Old1, $New)
}
elseif ($Text.Contains($Old2)) {
    $Text = $Text.Replace($Old2, $New)
}
elseif (-not $Text.Contains($New)) {
    throw "Expected V3/V3.2 painting tag declaration not found."
}

# Ensure required imports exist.
if (-not $Text.Contains("import net.minecraft.resources.ResourceLocation;")) {
    $anchor = "import net.minecraft.core.registries.Registries;"
    if (-not $Text.Contains($anchor)) {
        throw "Could not locate Registries import anchor."
    }
    $Text = $Text.Replace(
        $anchor,
        $anchor + "`r`nimport net.minecraft.resources.ResourceLocation;"
    )
}

if (-not $Text.Contains("import net.minecraft.tags.TagKey;")) {
    $anchor = "import net.minecraft.tags.PaintingVariantTags;"
    if ($Text.Contains($anchor)) {
        $Text = $Text.Replace(
            $anchor,
            $anchor + "`r`nimport net.minecraft.tags.TagKey;"
        )
    }
    else {
        $anchor2 = "import net.minecraft.resources.ResourceLocation;"
        $Text = $Text.Replace(
            $anchor2,
            $anchor2 + "`r`nimport net.minecraft.tags.TagKey;"
        )
    }
}

# PaintingVariantTags import is now unused; remove it if present.
$Text = $Text.Replace("import net.minecraft.tags.PaintingVariantTags;`r`n", "")
$Text = $Text.Replace("import net.minecraft.tags.PaintingVariantTags;`n", "")

if (-not $Text.Contains($New)) {
    throw "V3.3 validation failed: public TagKey.create declaration missing."
}

[IO.File]::WriteAllText($Target, $Text, $Utf8NoBom)

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.3 - TAGKEY FIX" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] Replaced private PaintingVariantTags.create() with public TagKey.create()." -ForegroundColor Green
Write-Host "[BUILD] Running compileJava + processResources..." -ForegroundColor Cyan

$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_3_BUILD_LAST.txt"
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
Write-Host "[OK] Custom Paintings V3.3 installed." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "In game:"
Write-Host "  /give @s domesurvival:memory_painting"
exit 0
