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
    "src\main\java\com\wasted\domesurvival\forge\quest\QuestActionEvents.java",
    "src\main\resources\assets\ftbquests\ftb_quests_theme.txt",
    "src\main\resources\assets\domesurvival\textures\gui\quests\chapter_06_power.png",
    "src\main\resources\assets\domesurvival\models\item\coal_generator.json",
    "src\main\resources\assets\domesurvival\models\item\basic_energy_pipe.json",
    "src\main\resources\assets\domesurvival\models\item\energy_buffer.json",
    "src\main\resources\assets\domesurvival\models\item\airlock_control_panel.json",
    "src\main\resources\assets\domesurvival\models\block\coal_generator.json",
    "src\main\resources\assets\domesurvival\models\block\basic_energy_pipe_core.json",
    "src\main\resources\assets\domesurvival\models\block\energy_buffer_v39_5.json",
    "src\main\resources\assets\domesurvival\models\block\airlock_control_panel.json",
    "src\main\resources\assets\domesurvival\blockstates\coal_generator.json",
    "src\main\resources\assets\domesurvival\blockstates\basic_energy_pipe.json",
    "src\main\resources\assets\domesurvival\blockstates\energy_buffer.json",
    "src\main\resources\assets\domesurvival\blockstates\airlock_control_panel.json",
    "src\main\resources\assets\domesurvival\textures\block\coal_generator_top.png",
    "src\main\resources\assets\domesurvival\textures\block\coal_generator_front.png",
    "src\main\resources\assets\domesurvival\textures\block\coal_generator_side.png",
    "src\main\resources\assets\domesurvival\textures\block\basic_energy_pipe.png",
    "src\main\resources\assets\domesurvival\textures\block\basic_energy_pipe_core.png",
    "src\main\resources\assets\domesurvival\textures\block\basic_energy_pipe_detail.png",
    "src\main\resources\assets\domesurvival\textures\block\energy_buffer\front_5.png",
    "src\main\resources\assets\domesurvival\textures\block\airlock_control_panel.png"
)

foreach ($relativeFile in $relativeFiles) {
    $sourcePath = Join-Path $payloadRoot $relativeFile
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Patch payload file is missing: $relativeFile"
    }
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupRoot = Join-Path $projectPath ("backups\phase8_2_chapter5_visual_resource_fix_" + $timestamp)
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

Write-Host "DomeSurvival Chapter 5 visual/resource patch installed." -ForegroundColor Green
Write-Host "Backup: $backupRoot"
Write-Host "Next run:"
Write-Host "  powershell -ExecutionPolicy Bypass -File `"$patchRoot\COMPILE_PHASE8_2_CHAPTER5_VISUAL_RESOURCE_FIX.ps1`" -Project `"$projectPath`""
Write-Host "  powershell -ExecutionPolicy Bypass -File `"$patchRoot\VERIFY_PHASE8_2_CHAPTER5_VISUAL_RESOURCE_FIX.ps1`" -Project `"$projectPath`""

