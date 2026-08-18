$ErrorActionPreference = "Stop"
$Root = (Resolve-Path $PSScriptRoot).Path

Write-Host "============================================================"
Write-Host "DomeSurvival Stage 4B V5.3 - approved NPC skins"
Write-Host "============================================================"
Write-Host ""

Add-Type -AssemblyName System.Drawing

$Map = @(
    @{ Npc = "maneogflow"; File = "src\main\resources\assets\domesurvival\textures\npc\expedition_soldier.png" },
    @{ Npc = "iVan"; File = "src\main\resources\assets\domesurvival\textures\npc\dome_security_officer.png" }
)

foreach ($entry in $Map) {
    $path = Join-Path $Root $entry.File
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Missing texture: $path"
    }

    $img = [System.Drawing.Image]::FromFile($path)
    try {
        if ($img.Width -ne 64 -or $img.Height -ne 64) {
            throw "Invalid skin size: $($img.Width)x$($img.Height) : $($entry.File)"
        }
    }
    finally {
        $img.Dispose()
    }

    Write-Host "[OK] $($entry.Npc) -> $($entry.File)" -ForegroundColor Green
}

Write-Host ""
Write-Host "[OK] Textures installed." -ForegroundColor Green
Write-Host "The existing NPC scripts already point to these resource locations."
Write-Host "Launch FULL DEV; no NPC recreation is required."
exit 0
