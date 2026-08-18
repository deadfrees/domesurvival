$ErrorActionPreference = "Stop"
$Root = (Resolve-Path $PSScriptRoot).Path
$World = Join-Path $Root "run\saves\WASTED_TEST"

if (-not (Test-Path -LiteralPath $World)) {
    throw "WASTED_TEST not found: $World"
}

$Target = Join-Path $World "customnpcs\scripts\ecmascript"
New-Item -ItemType Directory -Force -Path $Target | Out-Null

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backup = Join-Path $World "customnpcs\scripts\_domesurvival_backup\stage4b_v4_$stamp"
New-Item -ItemType Directory -Force -Path $backup | Out-Null

foreach ($name in @("ambient_expedition_soldier.js","ambient_security_officer.js")) {
    $existing = Join-Path $Target $name
    if (Test-Path -LiteralPath $existing) {
        Copy-Item -LiteralPath $existing -Destination (Join-Path $backup $name) -Force
    }

    $source = Join-Path $Root "CUSTOMNPCS_STAGE4\$name"
    Copy-Item -LiteralPath $source -Destination $existing -Force

    $a = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
    $b = (Get-FileHash -LiteralPath $existing -Algorithm SHA256).Hash
    if ($a -ne $b) {
        throw "Script verification failed: $name"
    }
}

Write-Host ""
Write-Host "[OK] Stage 4B V4 NPC scripts installed." -ForegroundColor Green
Write-Host "maneogflow -> expedition_soldier.png"
Write-Host "iVan       -> dome_security_officer.png"
Write-Host ""
Write-Host "Restart FULL DEV, then Reset Script/Apply once on both existing NPCs."
