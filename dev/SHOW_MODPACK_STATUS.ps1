$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

Write-Host 'DomeSurvival FULL DEV V6.8 status'
Write-Host '================================'
Write-Host ''

foreach ($rel in @(
    'run\mods',
    'run\mods_disabled',
    'run\mods_sync_quarantine',
    'run\mods_dev_hold',
    'run\mods_dev_hold_all',
    'dev\generated\fullmods'
)) {
    $path = Join-Path $ProjectRoot $rel
    $count = 0

    if (Test-Path -LiteralPath $path) {
        $count = @(Get-ChildItem -LiteralPath $path -File -Filter '*.jar' -ErrorAction SilentlyContinue).Count
    }

    Write-Host ("{0,-34} {1,4} JARs" -f $rel, $count)
}

Write-Host ''

$modsDir = Join-Path $ProjectRoot 'run\mods'
$jars = @()

if (Test-Path -LiteralPath $modsDir) {
    $jars = @(Get-ChildItem -LiteralPath $modsDir -File -Filter '*.jar')
}

foreach ($entry in @(
    @{Name='Curios'; Pattern='^curios-forge-'},
    @{Name='CoFH Core'; Pattern='^cofh_core-'},
    @{Name='Thermal Core'; Pattern='^thermal_core-'},
    @{Name='CustomNPCs'; Pattern='custom.*npcs.*\.jar$'},
    @{Name='Ad Astra'; Pattern='^ad_astra-'},
    @{Name='Architectury'; Pattern='^architectury-'}
)) {
    $match = $jars |
        Where-Object { $_.Name -match $entry.Pattern } |
        Select-Object -First 1

    if ($match) {
        Write-Host "[OK] $($entry.Name): $($match.Name)" -ForegroundColor Green
    } else {
        Write-Host "[MISSING] $($entry.Name)" -ForegroundColor Red
    }
}
