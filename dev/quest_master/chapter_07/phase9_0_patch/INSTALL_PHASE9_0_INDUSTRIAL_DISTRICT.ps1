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

$actionFiles = @(
    "ch7_intro.json",
    "ch7_purifier_placed.json",
    "ch7_purified_water_ready.json",
    "ch7_electrolyzer_placed.json",
    "ch7_oxygen_produced.json",
    "ch7_oxygen_line.json",
    "ch7_filler_placed.json",
    "ch7_tank_filled.json",
    "ch7_oxygen_sortie.json",
    "ch7_finale.json"
)

$relativeFiles = @(
    "dev\quest_master\chapter_07\CHAPTER7_DESIGN_V9_0.md",
    "dev\quest_master\chapter_07\CHAPTER7_REGISTRY_V9_0.json",
    "dev\quest_master\ftbquests\quests\chapters\76CBABB04B110F16.snbt",
    "dev\quest_master\visual\QUEST_BACKGROUND_MANIFEST_V9_0.json",
    "src\main\java\com\wasted\domesurvival\forge\quest\QuestActionEvents.java",
    "src\main\java\com\wasted\domesurvival\forge\quest\QuestGlobalRegistry.java",
    "src\main\resources\assets\ftbquests\ftb_quests_theme.txt",
    "src\main\resources\assets\domesurvival\textures\gui\quests\chapter_07_industrial_district.png",
    "src\main\resources\data\domesurvival\recipes\oxygen_mask.json"
)
foreach ($actionFile in $actionFiles) {
    $relativeFiles += "src\main\resources\data\domesurvival\advancements\quest_actions\$actionFile"
}

foreach ($relativeFile in $relativeFiles) {
    $sourcePath = Join-Path $payloadRoot $relativeFile
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Patch payload file is missing: $relativeFile"
    }
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupRoot = Join-Path $projectPath ("_manual_backups\phase9_0_industrial_district_" + $timestamp)
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

function Backup-And-Copy {
    param([string]$Source, [string]$Target, [string]$RelativeBackupPath)

    if (Test-Path -LiteralPath $Target -PathType Leaf) {
        $backupPath = Join-Path $backupRoot $RelativeBackupPath
        New-Item -ItemType Directory -Path (Split-Path -Parent $backupPath) -Force | Out-Null
        Copy-Item -LiteralPath $Target -Destination $backupPath -Force
    }

    New-Item -ItemType Directory -Path (Split-Path -Parent $Target) -Force | Out-Null
    Copy-Item -LiteralPath $Source -Destination $Target -Force
}

foreach ($relativeFile in $relativeFiles) {
    Backup-And-Copy -Source (Join-Path $payloadRoot $relativeFile) -Target (Join-Path $projectPath $relativeFile) -RelativeBackupPath $relativeFile
}

$runtimeRelative = "run\config\ftbquests\quests\chapters\76CBABB04B110F16.snbt"
$runtimeRoot = Split-Path -Parent (Join-Path $projectPath $runtimeRelative)
if (Test-Path -LiteralPath $runtimeRoot -PathType Container) {
    Backup-And-Copy -Source (Join-Path $payloadRoot "dev\quest_master\ftbquests\quests\chapters\76CBABB04B110F16.snbt") -Target (Join-Path $projectPath $runtimeRelative) -RelativeBackupPath $runtimeRelative
}

Write-Host "DomeSurvival Phase 9.0 Industrial District installed." -ForegroundColor Green
Write-Host "Backup: $backupRoot"
Write-Host "Run the compile helper and verifier from this patch directory."

