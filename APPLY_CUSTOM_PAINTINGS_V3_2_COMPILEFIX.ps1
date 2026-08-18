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
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_2_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Target -Destination (Join-Path $Backup "MemoryPaintingItem.java") -Force

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Text = [IO.File]::ReadAllText($Target,[Text.Encoding]::UTF8)

$Old = 'PaintingVariantTags.create(new ResourceLocation(DomeSurvival.MOD_ID, "memory_paintings"));'
$New = 'PaintingVariantTags.create(DomeSurvival.MOD_ID + ":memory_paintings");'

if ($Text.Contains($Old)) {
    $Text = $Text.Replace($Old,$New)
}
elseif (-not $Text.Contains($New)) {
    throw "Expected V3.1 painting tag line not found. Send MemoryPaintingItem.java before applying another patch."
}

# ResourceLocation is no longer needed in this class after the fix.
$Text = $Text.Replace("import net.minecraft.resources.ResourceLocation;`r`n","")
$Text = $Text.Replace("import net.minecraft.resources.ResourceLocation;`n","")

if (-not $Text.Contains($New)) {
    throw "Compile-fix validation failed: corrected PaintingVariantTags.create(String) call missing."
}

[IO.File]::WriteAllText($Target,$Text,$Utf8NoBom)

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.2 - COMPILE FIX" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] PaintingVariantTags.create now receives a String." -ForegroundColor Green
Write-Host "[BUILD] Running compileJava + processResources..." -ForegroundColor Cyan

$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_2_BUILD_LAST.txt"
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
    Write-Host "[ERROR] Build still failed." -ForegroundColor Red
    Write-Host "Log: $BuildLog" -ForegroundColor Red
    Write-Host "Backup: $Backup" -ForegroundColor Yellow
    exit $Exit
}

Write-Host ""
Write-Host "[OK] compileJava + processResources succeeded." -ForegroundColor Green
Write-Host "[OK] Custom Paintings V3.2 compile fix installed." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "In game:"
Write-Host "  /give @s domesurvival:memory_painting"
exit 0
