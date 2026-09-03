param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
)

$ErrorActionPreference = "Stop"
$sourceRoot = Join-Path $ProjectRoot "dev\lastworld"
$serverRoot = Join-Path $ProjectRoot "run\lastworld-server"
$worldRoot = Join-Path $serverRoot "LastWorld"

& (Join-Path $sourceRoot "BUILD_YULARI_COMPAT.ps1") -ProjectRoot $ProjectRoot

$directories = @(
    (Join-Path $serverRoot "config\lostcities\profiles"),
    (Join-Path $serverRoot "config\alexscaves_biome_generation"),
    (Join-Path $serverRoot "defaultconfigs"),
    (Join-Path $worldRoot "serverconfig"),
    (Join-Path $worldRoot "datapacks")
)
New-Item -ItemType Directory -Force -Path $directories | Out-Null

Copy-Item -LiteralPath (Join-Path $sourceRoot "config\lostcities\profiles\lastworld.json") -Destination (Join-Path $serverRoot "config\lostcities\profiles\lastworld.json") -Force
Copy-Item -LiteralPath (Join-Path $sourceRoot "config\lostcities-common.toml") -Destination (Join-Path $serverRoot "config\lostcities-common.toml") -Force
Copy-Item -Path (Join-Path $sourceRoot "config\alexscaves_biome_generation\*.json") -Destination (Join-Path $serverRoot "config\alexscaves_biome_generation") -Force
Copy-Item -LiteralPath (Join-Path $sourceRoot "defaultconfigs\lostcities-server.toml") -Destination (Join-Path $serverRoot "defaultconfigs\lostcities-server.toml") -Force
Copy-Item -LiteralPath (Join-Path $sourceRoot "defaultconfigs\lostcities-server.toml") -Destination (Join-Path $worldRoot "serverconfig\lostcities-server.toml") -Force

$targetDatapack = Join-Path $worldRoot "datapacks\LastWorld_Generation"
if (Test-Path -LiteralPath $targetDatapack) {
    Remove-Item -LiteralPath $targetDatapack -Recurse -Force
}
Copy-Item -LiteralPath (Join-Path $sourceRoot "datapack") -Destination $targetDatapack -Recurse -Force

Write-Host "[OK] LastWorld headless generation directory prepared: $serverRoot"
