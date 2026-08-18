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
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_5_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Target -Destination (Join-Path $Backup "MemoryPaintingItem.java") -Force

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Text = [IO.File]::ReadAllText($Target, [Text.Encoding]::UTF8)

# V3.4 referenced DomeSurvival.LOGGER, but DomeSurvival currently has no
# LOGGER field. Keep the diagnostic without introducing a new project-wide
# logging dependency or touching DomeSurvival.java.
$OldMultiline = @'
            DomeSurvival.LOGGER.error(
                    "Memory painting placement failed: no Dome Survival painting variants are loaded."
            );
'@

$NewMultiline = @'
            System.err.println(
                    "[DomeSurvival] Memory painting placement failed: "
                            + "no Dome Survival painting variants are loaded."
            );
'@

if ($Text.Contains($OldMultiline)) {
    $Text = $Text.Replace($OldMultiline, $NewMultiline)
}
elseif ($Text.Contains('DomeSurvival.LOGGER.error(')) {
    # Tolerate line-ending/format differences from partially modified sources.
    $Pattern = 'DomeSurvival\.LOGGER\.error\s*\(\s*"Memory painting placement failed: no Dome Survival painting variants are loaded\."\s*\)\s*;'
    $Replacement = 'System.err.println("[DomeSurvival] Memory painting placement failed: no Dome Survival painting variants are loaded.");'
    $Updated = [regex]::Replace($Text, $Pattern, $Replacement, 1)

    if ($Updated -eq $Text) {
        throw "Found DomeSurvival.LOGGER.error but could not safely replace the expected painting diagnostic."
    }
    $Text = $Updated
}

if ($Text.Contains('DomeSurvival.LOGGER')) {
    throw "V3.5 validation failed: DomeSurvival.LOGGER still remains in MemoryPaintingItem.java."
}

[IO.File]::WriteAllText($Target, $Text, $Utf8NoBom)

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.5 - LOGGER COMPILE FIX" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[OK] Removed invalid DomeSurvival.LOGGER reference." -ForegroundColor Green
Write-Host "[BUILD] Running clean build with full dev profile..." -ForegroundColor Cyan

$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_5_BUILD_LAST.txt"
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
Write-Host "[OK] Custom Paintings V3.5 compile fix installed." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "In game:"
Write-Host "  /give @s domesurvival:memory_painting"
exit 0
