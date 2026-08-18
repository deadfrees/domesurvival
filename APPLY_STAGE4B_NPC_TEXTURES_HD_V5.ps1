$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$World = Join-Path $Root "run\saves\WASTED_TEST"

Write-Host "============================================================"
Write-Host "DomeSurvival Stage 4B V5 - HD NPC textures"
Write-Host "============================================================"
Write-Host ""

$Textures = @(
    "src\main\resources\assets\domesurvival\textures\npc\expedition_soldier.png",
    "src\main\resources\assets\domesurvival\textures\npc\dome_security_officer.png"
)

Add-Type -AssemblyName System.Drawing

foreach ($rel in $Textures) {
    $path = Join-Path $Root $rel
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Missing texture: $path"
    }

    $img = [System.Drawing.Image]::FromFile($path)
    try {
        if ($img.Width -ne 4096 -or $img.Height -ne 4096) {
            throw "Invalid texture size for $rel : $($img.Width)x$($img.Height)"
        }
    }
    finally {
        $img.Dispose()
    }

    Write-Host "[OK] $rel = 4096x4096"
}

if (Test-Path -LiteralPath $World) {
    $Target = Join-Path $World "customnpcs\scripts\ecmascript"
    New-Item -ItemType Directory -Force -Path $Target | Out-Null

    $stamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $backup = Join-Path $World "customnpcs\scripts\_domesurvival_backup\stage4b_v5_$stamp"
    New-Item -ItemType Directory -Force -Path $backup | Out-Null

    foreach ($name in @(
        "ambient_expedition_soldier.js",
        "ambient_security_officer.js"
    )) {
        $source = Join-Path $Root "CUSTOMNPCS_STAGE4\$name"
        $target = Join-Path $Target $name

        if (Test-Path -LiteralPath $target) {
            Copy-Item -LiteralPath $target -Destination (Join-Path $backup $name) -Force
        }

        Copy-Item -LiteralPath $source -Destination $target -Force

        $srcHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
        $dstHash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
        if ($srcHash -ne $dstHash) {
            throw "Script verification failed: $name"
        }
    }

    Write-Host "[OK] WASTED_TEST NPC scripts refreshed."
}

Write-Host ""
Write-Host "NPC mapping:"
Write-Host "  maneogflow -> expedition_soldier.png"
Write-Host "  iVan       -> dome_security_officer.png"
Write-Host ""
Write-Host "[OK] Patch installed. Rebuild/restart FULL DEV."
