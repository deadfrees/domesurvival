param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
)

$ErrorActionPreference = "Stop"
$sourceRoot = Join-Path $ProjectRoot "dev\lastworld"
$runRoot = Join-Path $ProjectRoot "run"

$profileDir = Join-Path $runRoot "config\lostcities\profiles"
$defaultConfigDir = Join-Path $runRoot "defaultconfigs"
$configDir = Join-Path $runRoot "config"
$alexCavesConfigDir = Join-Path $configDir "alexscaves_biome_generation"

New-Item -ItemType Directory -Force -Path $profileDir, $defaultConfigDir, $configDir, $alexCavesConfigDir | Out-Null
Copy-Item -LiteralPath (Join-Path $sourceRoot "config\lostcities\profiles\lastworld.json") -Destination (Join-Path $profileDir "lastworld.json") -Force
Copy-Item -LiteralPath (Join-Path $sourceRoot "defaultconfigs\lostcities-server.toml") -Destination (Join-Path $defaultConfigDir "lostcities-server.toml") -Force
Copy-Item -LiteralPath (Join-Path $sourceRoot "config\lostcities-common.toml") -Destination (Join-Path $configDir "lostcities-common.toml") -Force
Copy-Item -Path (Join-Path $sourceRoot "config\alexscaves_biome_generation\*.json") -Destination $alexCavesConfigDir -Force

Write-Host "[OK] LastWorld Lost Cities profile installed."
Write-Host "Profile: lastworld"
Write-Host "Preset:  domesurvival:lastworld"
Write-Host "Alex's Caves: dry-world biome rules installed."
