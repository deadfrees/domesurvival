[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$patchRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$payloadRoot = Join-Path $patchRoot "payload"
$projectPath = (Resolve-Path -LiteralPath $Project).Path

if (-not (Test-Path -LiteralPath (Join-Path $projectPath "build.gradle") -PathType Leaf)) {
    throw "The target is not a DomeSurvival project: $projectPath"
}
if (-not (Test-Path -LiteralPath $payloadRoot -PathType Container)) {
    throw "Patch payload is missing: $payloadRoot"
}

$relativeFiles = @(
    "dev\quest_master\ftbquests\quests\chapters\4A2E731D5C9B684F.snbt",
    "dev\quest_master\chapter_06\CHAPTER6_REGISTRY_V8_0.json",
    "dev\quest_master\visual\QUEST_BACKGROUND_MANIFEST_V8_3.json",
    "src\main\resources\assets\ftbquests\ftb_quests_theme.txt",
    "src\main\resources\assets\domesurvival\textures\gui\quests\chapter_00_under_dome.png",
    "src\main\resources\assets\domesurvival\textures\gui\quests\chapter_01_first_days.png",
    "src\main\resources\assets\domesurvival\textures\gui\quests\chapter_02_beyond_gate.png",
    "src\main\resources\assets\domesurvival\textures\gui\quests\chapter_03_settlement.png",
    "src\main\resources\assets\domesurvival\textures\gui\quests\chapter_04_food_system.png",
    "src\main\resources\assets\domesurvival\textures\gui\quests\chapter_06_power.png"
)

foreach ($relativeFile in $relativeFiles) {
    $sourcePath = Join-Path $payloadRoot $relativeFile
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Patch payload file is missing: $relativeFile"
    }
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupRoot = Join-Path $projectPath ("backups\phase8_3_all_chapter_backgrounds_" + $timestamp)
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

function Backup-And-Copy {
    param(
        [string]$Source,
        [string]$Target,
        [string]$RelativeBackupPath
    )

    if (Test-Path -LiteralPath $Target -PathType Leaf) {
        $backupPath = Join-Path $backupRoot $RelativeBackupPath
        New-Item -ItemType Directory -Path (Split-Path -Parent $backupPath) -Force | Out-Null
        Copy-Item -LiteralPath $Target -Destination $backupPath -Force
    }

    New-Item -ItemType Directory -Path (Split-Path -Parent $Target) -Force | Out-Null
    Copy-Item -LiteralPath $Source -Destination $Target -Force
}

foreach ($relativeFile in $relativeFiles) {
    Backup-And-Copy `
        -Source (Join-Path $payloadRoot $relativeFile) `
        -Target (Join-Path $projectPath $relativeFile) `
        -RelativeBackupPath $relativeFile
}

$runtimeQuestDirectory = Join-Path $projectPath "run\config\ftbquests\quests\chapters"
if (Test-Path -LiteralPath $runtimeQuestDirectory -PathType Container) {
    $runtimeRelative = "run\config\ftbquests\quests\chapters\4A2E731D5C9B684F.snbt"
    Backup-And-Copy `
        -Source (Join-Path $payloadRoot "dev\quest_master\ftbquests\quests\chapters\4A2E731D5C9B684F.snbt") `
        -Target (Join-Path $projectPath $runtimeRelative) `
        -RelativeBackupPath $runtimeRelative
}

Write-Host "DomeSurvival Phase 8.3 backgrounds installed." -ForegroundColor Green
Write-Host "Backup: $backupRoot"
Write-Host "Next run the compile helper and verifier from this patch directory."
