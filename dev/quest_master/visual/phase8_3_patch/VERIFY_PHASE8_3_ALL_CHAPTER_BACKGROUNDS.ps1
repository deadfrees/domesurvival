[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$verificationErrors = @()

function Add-ErrorMessage {
    param([string]$Message)
    $script:verificationErrors += $Message
}

function Read-Utf8Text {
    param([string]$Path)
    return [System.IO.File]::ReadAllText($Path, [System.Text.Encoding]::UTF8)
}

$projectPath = (Resolve-Path -LiteralPath $Project).Path
$chapterPath = Join-Path $projectPath "dev\quest_master\ftbquests\quests\chapters\4A2E731D5C9B684F.snbt"
$runtimeChapterPath = Join-Path $projectPath "run\config\ftbquests\quests\chapters\4A2E731D5C9B684F.snbt"
$themePath = Join-Path $projectPath "src\main\resources\assets\ftbquests\ftb_quests_theme.txt"
$manifestPath = Join-Path $projectPath "dev\quest_master\visual\QUEST_BACKGROUND_MANIFEST_V8_3.json"

foreach ($requiredPath in @($chapterPath, $themePath, $manifestPath)) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        Add-ErrorMessage "Required file is missing: $requiredPath"
    }
}

$chapterText = ""
if (Test-Path -LiteralPath $chapterPath -PathType Leaf) {
    $chapterText = Read-Utf8Text -Path $chapterPath
}

if ($chapterText -notmatch '(?m)^  title:\s*".*Пусть горит свет.*"\s*$') {
    Add-ErrorMessage "The energy chapter title is not 'Пусть горит свет'."
}
if ($chapterText -match '(?m)^\s{2,4}title:\s*".*Глава 5') {
    Add-ErrorMessage "The 'Глава 5' prefix still appears in the chapter list or intro quest title."
}

$questCount = ([regex]::Matches($chapterText, '(?m)^    id:\s*"[0-9A-F]{16}"\s*$')).Count
if ($questCount -ne 20) {
    Add-ErrorMessage "Expected 20 energy quests, found $questCount."
}
$manualCheckmarks = ([regex]::Matches($chapterText, '(?im)type:\s*"(?:checkmark|manual)"')).Count
$unknownReferences = ([regex]::Matches($chapterText, '(?i)unknown|placeholder|missingno|minecraft:barrier')).Count
$mekanismIcons = ([regex]::Matches($chapterText, '(?im)^\s*icon:\s*"mekanism:')).Count
if ($manualCheckmarks -ne 0) {
    Add-ErrorMessage "Manual checkmarks found: $manualCheckmarks"
}
if ($unknownReferences -ne 0) {
    Add-ErrorMessage "Unknown or placeholder references found: $unknownReferences"
}
if ($mekanismIcons -ne 0) {
    Add-ErrorMessage "Mekanism placeholder icons remain: $mekanismIcons"
}
if ($chapterText -notmatch 'POWER_INFRASTRUCTURE_ESTABLISHED') {
    Add-ErrorMessage "Final energy flag is missing."
}

if (Test-Path -LiteralPath $runtimeChapterPath -PathType Leaf) {
    $masterHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $chapterPath).Hash
    $runtimeHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $runtimeChapterPath).Hash
    if ($masterHash -ne $runtimeHash) {
        Add-ErrorMessage "run/config energy chapter is not synchronized with quest master."
    }
}

$themeText = ""
if (Test-Path -LiteralPath $themePath -PathType Leaf) {
    $themeText = Read-Utf8Text -Path $themePath
}

$backgrounds = @(
    @{ Id = "5017105A9984A511"; File = "chapter_00_under_dome.png" },
    @{ Id = "38F6E366B367B563"; File = "chapter_01_first_days.png" },
    @{ Id = "643138B0A36D1017"; File = "chapter_02_beyond_gate.png" },
    @{ Id = "5E38036299F32A70"; File = "chapter_03_settlement.png" },
    @{ Id = "2D984F68E1AC5B77"; File = "chapter_04_food_system.png" },
    @{ Id = "4A2E731D5C9B684F"; File = "chapter_06_power.png" }
)

Add-Type -AssemblyName System.Drawing
$backgroundHashes = @()
$qualityRows = @()
foreach ($background in $backgrounds) {
    $sourceRelative = "assets\domesurvival\textures\gui\quests\" + $background.File
    $sourcePath = Join-Path $projectPath ("src\main\resources\" + $sourceRelative)
    $processedPath = Join-Path $projectPath ("build\resources\main\" + $sourceRelative)

    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        Add-ErrorMessage "Source background is missing: $($background.File)"
        continue
    }
    if (-not (Test-Path -LiteralPath $processedPath -PathType Leaf)) {
        Add-ErrorMessage "Processed background is missing: $($background.File)"
        continue
    }

    $sourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourcePath).Hash
    $processedHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $processedPath).Hash
    $backgroundHashes += $sourceHash
    if ($sourceHash -ne $processedHash) {
        Add-ErrorMessage "Processed resources contain an old copy: $($background.File)"
    }

    $image = [System.Drawing.Image]::FromFile($sourcePath)
    try {
        $width = $image.Width
        $height = $image.Height
        $aspectDifference = [Math]::Abs(([double]$width / [double]$height) - (16.0 / 9.0))
        if (($width -lt 1600) -or ($height -lt 900) -or ($aspectDifference -ge 0.01)) {
            Add-ErrorMessage "Background quality is too low: $($background.File) ${width}x${height}"
        }
        $qualityRows += "$($background.File)=${width}x${height}"
    }
    finally {
        $image.Dispose()
    }

    $themeSectionPattern = '\[' + [regex]::Escape($background.Id) + '\]'
    $resourcePattern = 'background:\s*domesurvival:textures/gui/quests/' + [regex]::Escape($background.File)
    if ($themeText -notmatch $themeSectionPattern) {
        Add-ErrorMessage "Theme chapter section is missing: $($background.Id)"
    }
    if ($themeText -notmatch $resourcePattern) {
        Add-ErrorMessage "Theme background binding is missing: $($background.File)"
    }
}

$uniqueBackgroundCount = @($backgroundHashes | Sort-Object -Unique).Count
if ($uniqueBackgroundCount -ne $backgrounds.Count) {
    Add-ErrorMessage "Chapter background images are not all unique."
}

if ($verificationErrors.Count -gt 0) {
    Write-Host "DomeSurvival Phase 8.3 background verification: FAILED" -ForegroundColor Red
    foreach ($message in $verificationErrors) {
        Write-Host "  - $message" -ForegroundColor Red
    }
    exit 1
}

Write-Host "DomeSurvival Phase 8.3 background verification: OK" -ForegroundColor Green
Write-Host "Energy chapter list title: Пусть горит свет"
Write-Host "Visible 'Глава 5' prefix: 0"
Write-Host "Energy chapter quests: $questCount"
Write-Host "Manual checkmarks: $manualCheckmarks"
Write-Host "Unknown references: $unknownReferences"
Write-Host "Mekanism placeholder icons: $mekanismIcons"
Write-Host "High-detail unique backgrounds: $uniqueBackgroundCount"
foreach ($qualityRow in $qualityRows) {
    Write-Host "  $qualityRow"
}
Write-Host "Processed resource copies: synchronized"
Write-Host "Final flag: POWER_INFRASTRUCTURE_ESTABLISHED"

