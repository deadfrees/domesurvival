$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path

Write-Host "============================================================"
Write-Host "DomeSurvival Stage 4B V5.1 - NPC texture hotfix"
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

    Write-Host "[OK] $rel = 4096x4096" -ForegroundColor Green
}

Write-Host ""
Write-Host "NPC mapping:"
Write-Host "  maneogflow -> expedition_soldier.png"
Write-Host "  iVan       -> dome_security_officer.png"
Write-Host ""
Write-Host "[OK] Texture patch is installed." -ForegroundColor Green
Write-Host "No CustomNPCs world-script directories are modified by V5.1."
Write-Host "Run FULL DEV and Reset Script / Apply once on both existing NPCs if needed."
exit 0
