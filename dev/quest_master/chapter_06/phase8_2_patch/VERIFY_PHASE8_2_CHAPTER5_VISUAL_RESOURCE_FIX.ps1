[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"

function Add-VerificationError {
    param([string]$Message)
    $script:VerificationErrors += $Message
}

function Read-Utf8Text {
    param([string]$Path)
    return [System.IO.File]::ReadAllText($Path, [System.Text.Encoding]::UTF8)
}

function Test-RequiredFile {
    param(
        [string]$Path,
        [string]$Label
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        Add-VerificationError "$Label is missing: $Path"
        return $false
    }

    return $true
}

$VerificationErrors = @()
$projectPath = (Resolve-Path -LiteralPath $Project).Path
$chapterRelative = "dev\quest_master\ftbquests\quests\chapters\4A2E731D5C9B684F.snbt"
$chapterPath = Join-Path $projectPath $chapterRelative
$themePath = Join-Path $projectPath "src\main\resources\assets\ftbquests\ftb_quests_theme.txt"
$backgroundRelative = "assets\domesurvival\textures\gui\quests\chapter_06_power.png"
$backgroundPath = Join-Path $projectPath ("src\main\resources\" + $backgroundRelative)
$processedBackgroundPath = Join-Path $projectPath ("build\resources\main\" + $backgroundRelative)
$questJavaRoot = Join-Path $projectPath "src\main\java\com\wasted\domesurvival\forge\quest"
$questActionPath = Join-Path $questJavaRoot "QuestActionEvents.java"
$modBlocksPath = Join-Path $projectPath "src\main\java\com\wasted\domesurvival\forge\block\ModBlocks.java"
$airlockRegistryPath = Join-Path $projectPath "src\main\java\com\wasted\domesurvival\forge\airlock\AirlockPanelRegistry.java"

$chapterText = ""
if (Test-RequiredFile -Path $chapterPath -Label "Chapter SNBT") {
    $chapterText = Read-Utf8Text -Path $chapterPath
}

$themeText = ""
if (Test-RequiredFile -Path $themePath -Label "FTB Quests theme") {
    $themeText = Read-Utf8Text -Path $themePath
}

$questActionText = ""
if (Test-RequiredFile -Path $questActionPath -Label "Quest action integration") {
    $questActionText = Read-Utf8Text -Path $questActionPath
}

if ($chapterText -notmatch 'id:\s*"4A2E731D5C9B684F"') {
    Add-VerificationError "Chapter ID 4A2E731D5C9B684F was not found."
}

if ($chapterText -notmatch 'Глава 5 — «Пусть горит свет»') {
    Add-VerificationError "Visible Chapter 5 title is missing."
}

$oldRemovalNoticeCount = ([regex]::Matches($chapterText, 'Глава 5 удалена', 'IgnoreCase')).Count
$oldChapterNumberCount = ([regex]::Matches($chapterText, 'Глава 6|Главы 6', 'IgnoreCase')).Count
if ($oldRemovalNoticeCount -ne 0) {
    Add-VerificationError "The old Chapter 5 removal notice is still player-visible."
}
if ($oldChapterNumberCount -ne 0) {
    Add-VerificationError "Player-visible Chapter 6 wording remains in the Chapter 5 SNBT."
}

$questCount = ([regex]::Matches($chapterText, '(?m)^    id:\s*"[0-9A-F]{16}"\s*$')).Count
if ($questCount -ne 20) {
    Add-VerificationError "Expected 20 quests, found $questCount."
}

if ($chapterText -notmatch 'POWER_INFRASTRUCTURE_ESTABLISHED') {
    Add-VerificationError "Final flag POWER_INFRASTRUCTURE_ESTABLISHED is missing."
}

$manualCheckmarkCount = ([regex]::Matches($chapterText, '(?im)type:\s*"(?:checkmark|manual)"')).Count
if ($manualCheckmarkCount -ne 0) {
    Add-VerificationError "Manual checkmark tasks found: $manualCheckmarkCount."
}

$unknownIconCount = ([regex]::Matches($chapterText, '(?i)unknown|placeholder|missingno|minecraft:barrier')).Count
if ($unknownIconCount -ne 0) {
    Add-VerificationError "Unknown or placeholder quest references found: $unknownIconCount."
}

$mekanismIconCount = ([regex]::Matches($chapterText, '(?im)^\s*icon:\s*"mekanism:')).Count
if ($mekanismIconCount -ne 0) {
    Add-VerificationError "Mekanism placeholder icons remain in the DomeSurvival power chapter: $mekanismIconCount."
}

$expectedIcons = @(
    "coal_generator",
    "basic_energy_pipe",
    "energy_buffer",
    "airlock_control_panel"
)

$iconMatches = [regex]::Matches($chapterText, '(?im)^\s*icon:\s*"domesurvival:([a-z0-9_]+)"')
$chapterIconIds = @($iconMatches | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
foreach ($expectedIcon in $expectedIcons) {
    if ($chapterIconIds -notcontains $expectedIcon) {
        Add-VerificationError "Canonical quest icon is not used: domesurvival:$expectedIcon"
    }
}

$registrationText = ""
foreach ($registryPath in @($modBlocksPath, $airlockRegistryPath)) {
    if (Test-RequiredFile -Path $registryPath -Label "Item registry source") {
        $registrationText += "`n" + (Read-Utf8Text -Path $registryPath)
    }
}

$missingItemModels = 0
foreach ($iconId in $chapterIconIds) {
    $registrationPattern = 'ITEMS\.register\s*\(\s*"' + [regex]::Escape($iconId) + '"'
    if ($registrationText -notmatch $registrationPattern) {
        Add-VerificationError "No real item registration found for domesurvival:$iconId"
    }

    $itemModelRelative = "assets\domesurvival\models\item\$iconId.json"
    $itemModelPath = Join-Path $projectPath ("src\main\resources\" + $itemModelRelative)
    $processedItemModelPath = Join-Path $projectPath ("build\resources\main\" + $itemModelRelative)
    $blockstatePath = Join-Path $projectPath "src\main\resources\assets\domesurvival\blockstates\$iconId.json"

    if (-not (Test-RequiredFile -Path $itemModelPath -Label "Item model for domesurvival:$iconId")) {
        $missingItemModels++
        continue
    }
    if (-not (Test-RequiredFile -Path $processedItemModelPath -Label "Processed item model for domesurvival:$iconId")) {
        $missingItemModels++
    }
    Test-RequiredFile -Path $blockstatePath -Label "Blockstate for domesurvival:$iconId" | Out-Null

    try {
        $itemModel = (Read-Utf8Text -Path $itemModelPath) | ConvertFrom-Json
    }
    catch {
        Add-VerificationError "Invalid item model JSON for domesurvival:$iconId"
        continue
    }

    $parent = [string]$itemModel.parent
    if ($parent -notmatch '^domesurvival:block/(.+)$') {
        Add-VerificationError "Unexpected item model parent for domesurvival:${iconId}: $parent"
        continue
    }

    $blockModelName = $Matches[1]
    $blockModelRelative = "assets\domesurvival\models\block\$blockModelName.json"
    $blockModelPath = Join-Path $projectPath ("src\main\resources\" + $blockModelRelative)
    $processedBlockModelPath = Join-Path $projectPath ("build\resources\main\" + $blockModelRelative)
    if (-not (Test-RequiredFile -Path $blockModelPath -Label "Block model $blockModelName for domesurvival:$iconId")) {
        continue
    }
    Test-RequiredFile -Path $processedBlockModelPath -Label "Processed block model $blockModelName" | Out-Null

    try {
        $blockModel = (Read-Utf8Text -Path $blockModelPath) | ConvertFrom-Json
    }
    catch {
        Add-VerificationError "Invalid block model JSON: $blockModelName"
        continue
    }

    if ($blockModel.textures) {
        foreach ($textureProperty in $blockModel.textures.PSObject.Properties) {
            $textureId = [string]$textureProperty.Value
            if ($textureId -match '^domesurvival:block/(.+)$') {
                $textureName = $Matches[1]
                $textureRelative = "assets\domesurvival\textures\block\$textureName.png"
                $texturePath = Join-Path $projectPath ("src\main\resources\" + $textureRelative)
                $processedTexturePath = Join-Path $projectPath ("build\resources\main\" + $textureRelative)
                Test-RequiredFile -Path $texturePath -Label "Texture $textureName for $blockModelName" | Out-Null
                Test-RequiredFile -Path $processedTexturePath -Label "Processed texture $textureName" | Out-Null
            }
        }
    }
}

$backgroundWidth = 0
$backgroundHeight = 0
$backgroundQualityOk = $false
if (Test-RequiredFile -Path $backgroundPath -Label "Chapter background") {
    try {
        Add-Type -AssemblyName System.Drawing
        $backgroundImage = [System.Drawing.Image]::FromFile($backgroundPath)
        try {
            $backgroundWidth = $backgroundImage.Width
            $backgroundHeight = $backgroundImage.Height
            $aspect = [double]$backgroundWidth / [double]$backgroundHeight
            $aspectDifference = [Math]::Abs($aspect - (16.0 / 9.0))
            $backgroundQualityOk = ($backgroundWidth -ge 1600) -and ($backgroundHeight -ge 900) -and ($aspectDifference -lt 0.01)
        }
        finally {
            $backgroundImage.Dispose()
        }
    }
    catch {
        Add-VerificationError "Background could not be decoded as PNG."
    }
}

if (-not $backgroundQualityOk) {
    Add-VerificationError "Background quality is below the accepted high-resolution 16:9 threshold: ${backgroundWidth}x${backgroundHeight}."
}

Test-RequiredFile -Path $processedBackgroundPath -Label "Processed chapter background" | Out-Null
if ((Test-Path -LiteralPath $backgroundPath -PathType Leaf) -and (Test-Path -LiteralPath $processedBackgroundPath -PathType Leaf)) {
    $sourceBackgroundHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $backgroundPath).Hash
    $processedBackgroundHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $processedBackgroundPath).Hash
    if ($sourceBackgroundHash -ne $processedBackgroundHash) {
        Add-VerificationError "Processed resources still contain an old Chapter 5 background copy."
    }
}
if ($themeText -notmatch '\[4A2E731D5C9B684F\]') {
    Add-VerificationError "Theme section for Chapter 5 is missing."
}
if ($themeText -notmatch 'background:\s*domesurvival:textures/gui/quests/chapter_06_power\.png') {
    Add-VerificationError "Native per-chapter FTB background binding is missing."
}

if ($questActionText -notmatch 'ForgeCapabilities\.ENERGY') {
    Add-VerificationError "Forge Energy capability integration is missing."
}
foreach ($exactMarker in @(
    'isOurCanonicalGenerator',
    'isOurCanonicalEnergyLink',
    'isOurCanonicalEnergyStorage',
    'coal_generator',
    'basic_energy_pipe',
    'energy_buffer'
)) {
    if ($questActionText -notmatch [regex]::Escape($exactMarker)) {
        Add-VerificationError "Exact DomeSurvival energy detection marker is missing: $exactMarker"
    }
}
foreach ($fallbackMarker in @('generator', 'dynamo', 'alternator', 'energy_cable', 'energy_wire', 'energy_conduit', 'battery', 'capacitor', 'accumulator', 'energy_cell')) {
    if ($questActionText -notmatch [regex]::Escape($fallbackMarker)) {
        Add-VerificationError "Compatibility fallback marker is missing: $fallbackMarker"
    }
}

$allQuestJava = ""
if (Test-Path -LiteralPath $questJavaRoot -PathType Container) {
    foreach ($javaFile in Get-ChildItem -LiteralPath $questJavaRoot -Filter "*.java" -Recurse) {
        $allQuestJava += "`n" + (Read-Utf8Text -Path $javaFile.FullName)
    }
}
if ($allQuestJava -match '\bCH5_') {
    Add-VerificationError "Removed CH5_* action names are still present in quest Java."
}

$runtimeChapterPath = Join-Path $projectPath "run\config\ftbquests\quests\chapters\4A2E731D5C9B684F.snbt"
if (Test-Path -LiteralPath $runtimeChapterPath -PathType Leaf) {
    $masterChapterHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $chapterPath).Hash
    $runtimeChapterHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $runtimeChapterPath).Hash
    if ($masterChapterHash -ne $runtimeChapterHash) {
        Add-VerificationError "Active run/config Chapter 5 is not synchronized with the quest master."
    }
}

if ($VerificationErrors.Count -gt 0) {
    Write-Host "DomeSurvival Chapter 5 visual/resource verification: FAILED" -ForegroundColor Red
    foreach ($verificationError in $VerificationErrors) {
        Write-Host "  - $verificationError" -ForegroundColor Red
    }
    exit 1
}

Write-Host "DomeSurvival Chapter 5 visual/resource verification: OK" -ForegroundColor Green
Write-Host "Visible chapter number: 5"
Write-Host "Chapter quests: $questCount"
Write-Host "Old removal notice: $oldRemovalNoticeCount"
Write-Host "Old Chapter 6 wording: $oldChapterNumberCount"
Write-Host "Manual checkmarks: $manualCheckmarkCount"
Write-Host "Background: OK"
Write-Host "Background quality: OK (${backgroundWidth}x${backgroundHeight})"
Write-Host "DomeSurvival quest icons: OK ($($chapterIconIds -join ', '))"
Write-Host "Missing item models: $missingItemModels"
Write-Host "Unknown quest icons: $unknownIconCount"
Write-Host "Forge Energy integration: OK"
Write-Host "Exact DomeSurvival registry detection: OK"
Write-Host "Compatibility fallback detection: OK"
Write-Host "Final flag: POWER_INFRASTRUCTURE_ESTABLISHED"
