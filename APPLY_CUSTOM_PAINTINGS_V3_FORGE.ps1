$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Payload = Join-Path $Root "PATCH_PAYLOAD"
$ModItems = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\item\ModItems.java"
$CreativeTabs = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\item\ModCreativeTabs.java"
$RuLang = Join-Path $Root "src\main\resources\assets\domesurvival\lang\ru_ru.json"
$EnLang = Join-Path $Root "src\main\resources\assets\domesurvival\lang\en_us.json"
$Gradle = Join-Path $Root "gradlew.bat"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.1 - FORGE NATIVE" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

foreach ($required in @($Payload, $ModItems, $CreativeTabs, $Gradle)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Required project path is missing: $required"
    }
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null

function Backup-File {
    param([string]$Path, [string]$Name)
    if (Test-Path -LiteralPath $Path) {
        Copy-Item -LiteralPath $Path -Destination (Join-Path $Backup $Name) -Force
    }
}

function Merge-LangEntry {
    param(
        [string]$Path,
        [string]$Key,
        [string]$Value
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        $dir = Split-Path -Parent $Path
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
        $escaped = $Value.Replace('\','\\').Replace('"','\"')
        [IO.File]::WriteAllText(
            $Path,
            "{`r`n    `"$Key`": `"$escaped`"`r`n}`r`n",
            (New-Object System.Text.UTF8Encoding($false))
        )
        return
    }

    $text = [IO.File]::ReadAllText($Path, [Text.Encoding]::UTF8)
    if ($text.Contains('"' + $Key + '"')) {
        return
    }

    $close = $text.LastIndexOf("}")
    if ($close -lt 0) {
        throw "Language JSON has no closing brace: $Path"
    }

    $prefix = $text.Substring(0, $close).TrimEnd()
    $separator = if ($prefix.EndsWith("{") -or $prefix.EndsWith(",")) { "" } else { "," }
    $escaped = $Value.Replace('\','\\').Replace('"','\"')
    $updated = $prefix + $separator + "`r`n    `"$Key`": `"$escaped`"`r`n}`r`n"
    [IO.File]::WriteAllText($Path, $updated, (New-Object System.Text.UTF8Encoding($false)))
}

Backup-File $ModItems "ModItems.java"
Backup-File $CreativeTabs "ModCreativeTabs.java"
Backup-File $RuLang "ru_ru.json"
Backup-File $EnLang "en_us.json"

# Patch actual Forge item registry.
$itemsText = [IO.File]::ReadAllText($ModItems, [Text.Encoding]::UTF8)
if (-not $itemsText.Contains('MEMORY_PAINTING')) {
    $anchor = "    private ModItems() {"
    if (-not $itemsText.Contains($anchor)) {
        throw "Could not find ModItems insertion anchor."
    }

    $field = @'
    /** Separate painting item backed by Dome Survival painting variants. */
    public static final RegistryObject<Item> MEMORY_PAINTING = ITEMS.register(
            "memory_painting",
            () -> new MemoryPaintingItem(new Item.Properties().stacksTo(64))
    );

'@
    $itemsText = $itemsText.Replace($anchor, $field + $anchor)
    [IO.File]::WriteAllText($ModItems, $itemsText, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "[OK] Registered domesurvival:memory_painting in ModItems." -ForegroundColor Green
} else {
    Write-Host "[OK] ModItems already contains MEMORY_PAINTING." -ForegroundColor Green
}

# Give the item a stable authored position in the existing Dome Survival creative tab.
$tabText = [IO.File]::ReadAllText($CreativeTabs, [Text.Encoding]::UTF8)
if (-not $tabText.Contains('"memory_painting"')) {
    $tabAnchor = '            "airlock_binding_key",'
    if ($tabText.Contains($tabAnchor)) {
        $tabText = $tabText.Replace(
            $tabAnchor,
            $tabAnchor + "`r`n" + '            "memory_painting",'
        )
        [IO.File]::WriteAllText($CreativeTabs, $tabText, (New-Object System.Text.UTF8Encoding($false)))
        Write-Host "[OK] Added painting to Dome Survival creative order." -ForegroundColor Green
    } else {
        Write-Host "[WARN] Creative order anchor not found; registry-driven tab will still include the item." -ForegroundColor Yellow
    }
}

# Copy Java/resource payload into the real Forge source tree.
Get-ChildItem -LiteralPath $Payload -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($Payload.Length).TrimStart('\','/')
    $destination = Join-Path $Root $relative
    $destinationDir = Split-Path -Parent $destination
    New-Item -ItemType Directory -Force -Path $destinationDir | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}

Merge-LangEntry $RuLang "item.domesurvival.memory_painting" "Картина воспоминаний"
Merge-LangEntry $EnLang "item.domesurvival.memory_painting" "Memory Painting"

Write-Host "[OK] Copied 22 painting variants/textures into src/main/resources." -ForegroundColor Green
Write-Host "[OK] Recipe: minecraft:painting -> domesurvival:memory_painting." -ForegroundColor Green
Write-Host ""

# Compile against the user's exact Forge 47.4.x mapped workspace.
$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_BUILD_LAST.txt"
Write-Host "[BUILD] Verifying Java + resources..." -ForegroundColor Cyan

# PowerShell 5.1 may convert harmless native STDERR warnings into
# NativeCommandError records. Because this installer uses
# $ErrorActionPreference = "Stop", such a warning can abort the script before
# Gradle's real exit code is inspected. Redirect stdout/stderr inside cmd.exe
# instead, then use only Gradle's process exit code as the build verdict.
$BuildLogName = Split-Path -Leaf $BuildLog
Push-Location $Root
try {
    & cmd.exe /d /c "gradlew.bat compileJava processResources --no-daemon > `"$BuildLogName`" 2>&1"
    $buildExit = $LASTEXITCODE
}
finally {
    Pop-Location
}

if ($buildExit -ne 0) {
    Write-Host "[ERROR] Build verification failed. Project backup was kept at:" -ForegroundColor Red
    Write-Host "        $Backup" -ForegroundColor Red
    Write-Host "[ERROR] Full build output:" -ForegroundColor Red
    Write-Host "        $BuildLog" -ForegroundColor Red
    throw "CUSTOM PAINTINGS V3 build failed. Send CUSTOM_PAINTINGS_V3_BUILD_LAST.txt."
}

Write-Host "[OK] compileJava + processResources succeeded." -ForegroundColor Green

# Quarantine only the stale files created by our old KubeJS painting patches.
$StalePaths = @(
    "kubejs\startup_scripts\domesurvival_custom_paintings_startup.js",
    "kubejs\server_scripts\domesurvival_custom_paintings.js",
    "kubejs\data\domesurvival\painting_variant",
    "kubejs\assets\domesurvival\textures\painting",
    "kubejs\assets\domesurvival\textures\item\memory_painting.png",
    "run\kubejs\startup_scripts\domesurvival_custom_paintings_startup.js",
    "run\kubejs\server_scripts\domesurvival_custom_paintings.js",
    "run\kubejs\data\domesurvival\painting_variant",
    "run\kubejs\assets\domesurvival\textures\painting",
    "run\kubejs\assets\domesurvival\textures\item\memory_painting.png"
)

$StaleBackup = Join-Path $Backup "old_kubejs_paintings"
foreach ($rel in $StalePaths) {
    $path = Join-Path $Root $rel
    if (-not (Test-Path -LiteralPath $path)) {
        continue
    }

    $safeName = $rel.Replace('\','__').Replace('/','__')
    New-Item -ItemType Directory -Force -Path $StaleBackup | Out-Null
    Move-Item -LiteralPath $path -Destination (Join-Path $StaleBackup $safeName) -Force
    Write-Host "[CLEAN] Quarantined old broken KubeJS path: $rel" -ForegroundColor DarkYellow
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "[OK] CUSTOM PAINTINGS V3.1 FORGE NATIVE INSTALLED" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host "Item ID: domesurvival:memory_painting"
Write-Host "Variants: 22"
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "In game test:"
Write-Host "  /give @s domesurvival:memory_painting"
Write-Host ""
Write-Host "Then place it on a sufficiently large vertical wall."
exit 0
