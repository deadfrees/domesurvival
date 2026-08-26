[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$projectPath = (Resolve-Path -LiteralPath $Project).Path
$chapterRoot = Join-Path $projectPath "dev\quest_master\ftbquests\quests\chapters"
$runtimeRoot = Join-Path $projectPath "run\config\ftbquests\quests\chapters"

if (-not (Test-Path -LiteralPath (Join-Path $projectPath "build.gradle") -PathType Leaf)) {
    throw "The target is not a DomeSurvival project: $projectPath"
}

if (-not (Test-Path -LiteralPath $runtimeRoot -PathType Container)) {
    Write-Host "Runtime quest directory is absent; source quests are already updated." -ForegroundColor Yellow
    exit 0
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupRoot = Join-Path $projectPath ("_manual_backups\phase10_0_technology_" + $timestamp)
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

foreach ($file in @("38F6E366B367B563.snbt", "4A2E731D5C9B684F.snbt", "76CBABB04B110F16.snbt")) {
    $source = Join-Path $chapterRoot $file
    $target = Join-Path $runtimeRoot $file
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
        throw "Source quest is missing: $source"
    }
    if (Test-Path -LiteralPath $target -PathType Leaf) {
        Copy-Item -LiteralPath $target -Destination (Join-Path $backupRoot $file) -Force
    }
    Copy-Item -LiteralPath $source -Destination $target -Force
}

$builtJar = Join-Path $projectPath "build\libs\domesurvival-0.1.1.jar"
$runtimeJar = Join-Path $projectPath "run\mods\domesurvival-0.1.1-dev.jar"
if (Test-Path -LiteralPath $builtJar -PathType Leaf) {
    if (Test-Path -LiteralPath $runtimeJar -PathType Leaf) {
        Copy-Item -LiteralPath $runtimeJar -Destination (Join-Path $backupRoot "domesurvival-0.1.1-dev.jar") -Force
    }
    Copy-Item -LiteralPath $builtJar -Destination $runtimeJar -Force
    Write-Host "Runtime mod JAR updated." -ForegroundColor Green
} else {
    Write-Host "Built JAR is absent; quest files were installed without replacing the runtime mod." -ForegroundColor Yellow
}

Write-Host "DomeSurvival technology quest rewards installed." -ForegroundColor Green
Write-Host "Backup: $backupRoot"
