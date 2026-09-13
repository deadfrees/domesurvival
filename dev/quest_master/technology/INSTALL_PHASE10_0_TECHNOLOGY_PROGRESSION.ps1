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

foreach ($sourceFile in Get-ChildItem -LiteralPath $chapterRoot -Filter "*.snbt" -File | Sort-Object Name) {
    $file = $sourceFile.Name
    $source = $sourceFile.FullName
    $target = Join-Path $runtimeRoot $file
    if (Test-Path -LiteralPath $target -PathType Leaf) {
        Copy-Item -LiteralPath $target -Destination (Join-Path $backupRoot $file) -Force
    }
    Copy-Item -LiteralPath $source -Destination $target -Force
}

$versionLine = Get-Content -LiteralPath (Join-Path $projectPath "gradle.properties") |
    Where-Object { $_ -match '^mod_version=' } | Select-Object -First 1
if (-not $versionLine) {
    throw "mod_version is missing from gradle.properties"
}
$modVersion = ($versionLine -split '=', 2)[1].Trim()
$builtJar = Join-Path $projectPath "build\libs\domesurvival-$modVersion.jar"
$runtimeJar = Join-Path $projectPath "run\mods\domesurvival-$modVersion-dev.jar"
if (Test-Path -LiteralPath $builtJar -PathType Leaf) {
    Get-ChildItem -LiteralPath (Join-Path $projectPath "run\mods") -Filter "domesurvival-*.jar" -File |
        Where-Object { $_.FullName -ne $runtimeJar } |
        ForEach-Object {
            Move-Item -LiteralPath $_.FullName -Destination (Join-Path $backupRoot $_.Name) -Force
        }
    if (Test-Path -LiteralPath $runtimeJar -PathType Leaf) {
        Copy-Item -LiteralPath $runtimeJar -Destination (Join-Path $backupRoot (Split-Path $runtimeJar -Leaf)) -Force
    }
    Copy-Item -LiteralPath $builtJar -Destination $runtimeJar -Force
    Write-Host "Runtime mod JAR updated." -ForegroundColor Green
} else {
    Write-Host "Built JAR is absent; quest files were installed without replacing the runtime mod." -ForegroundColor Yellow
}

Write-Host "DomeSurvival technology quest rewards installed." -ForegroundColor Green
Write-Host "Backup: $backupRoot"
