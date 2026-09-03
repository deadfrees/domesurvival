param(
    [string]$SourceWorld = "WASTED_TEST",
    [string]$TargetWorld = "LastWorld",
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
)

$ErrorActionPreference = "Stop"
$runRoot = Join-Path $ProjectRoot "run"
$sourceRoot = Join-Path $runRoot ("saves\" + $SourceWorld)
$targetRoot = Join-Path $runRoot ("saves\" + $TargetWorld)
$sourceStructures = Join-Path $sourceRoot "generated\domesurvival\structures\lastworld"
$targetStructures = Join-Path $targetRoot "generated\domesurvival\structures\lastworld"
$sourceDatapack = Join-Path $ProjectRoot "dev\lastworld\datapack"
$targetDatapack = Join-Path $targetRoot "datapacks\LastWorld_Generation"

if (Get-Process -Name java, javaw -ErrorAction SilentlyContinue) {
    throw "Close Minecraft/Java before copying LastWorld transfer data."
}
if (-not (Test-Path -LiteralPath $sourceStructures -PathType Container)) {
    throw "Export not found. Open $SourceWorld and run: /dome lastworld export"
}
if (-not (Test-Path -LiteralPath $targetRoot -PathType Container)) {
    throw "Target world not found: $targetRoot"
}

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupRoot = Join-Path $ProjectRoot ("_manual_backups\lastworld_transfer_" + $stamp)
New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
if (Test-Path -LiteralPath $targetStructures) {
    Copy-Item -LiteralPath $targetStructures -Destination (Join-Path $backupRoot "lastworld_structures") -Recurse -Force
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $targetStructures) | Out-Null
Copy-Item -LiteralPath $sourceStructures -Destination $targetStructures -Recurse -Force

if (Test-Path -LiteralPath $targetDatapack) {
    Copy-Item -LiteralPath $targetDatapack -Destination (Join-Path $backupRoot "LastWorld_Generation") -Recurse -Force
}
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $targetDatapack) | Out-Null
Copy-Item -LiteralPath $sourceDatapack -Destination $targetDatapack -Recurse -Force

$sourceNpc = Join-Path $sourceRoot "customnpcs"
$targetNpc = Join-Path $targetRoot "customnpcs"
if (Test-Path -LiteralPath $sourceNpc -PathType Container) {
    if (Test-Path -LiteralPath $targetNpc) {
        Copy-Item -LiteralPath $targetNpc -Destination (Join-Path $backupRoot "customnpcs") -Recurse -Force
    }
    Copy-Item -LiteralPath $sourceNpc -Destination $targetNpc -Recurse -Force
}

Write-Host "[OK] Transfer files copied to $TargetWorld."
Write-Host "[OK] LastWorld generation datapack installed."
Write-Host "Open $TargetWorld and run: /dome lastworld import"
Write-Host "Backup: $backupRoot"
