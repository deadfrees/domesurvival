$ErrorActionPreference = "Stop"
$Root = (Resolve-Path $PSScriptRoot).Path

Write-Host "============================================================"
Write-Host "DomeSurvival Stage 4B V5.2 - native NPC UV texture fix"
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
            throw "Invalid native Minecraft skin size: $($img.Width)x$($img.Height) : $($entry.File)"
        }
    }
    finally {
        $img.Dispose()
    }

    Write-Host "[OK] $($entry.Npc) -> 64x64 native UV" -ForegroundColor Green
}

Write-Host ""
Write-Host "[OK] V5.2 installed." -ForegroundColor Green
Write-Host "Run FULL DEV. For existing NPCs use Reset Script / Apply once."
exit 0
