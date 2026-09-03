[CmdletBinding()]
param([string]$Project = "C:\domesurvival")

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$projectPath = (Resolve-Path -LiteralPath $Project).Path
$referenceRoot = Join-Path $projectPath "dev\art_reference"
$textureRoot = Join-Path $projectPath "src\main\resources\assets\domesurvival\textures\block\metallurgy"
$basePath = Join-Path $referenceRoot "metal_furnace_base_128.png"
$litPath = Join-Path $referenceRoot "metal_furnace_lit_128.png"

if (-not (Test-Path -LiteralPath $basePath -PathType Leaf) -or
    -not (Test-Path -LiteralPath $litPath -PathType Leaf)) {
    throw "Prepared Metal Furnace texture sources are missing in $referenceRoot"
}

New-Item -ItemType Directory -Force -Path $textureRoot | Out-Null
$base = [System.Drawing.Bitmap]::new($basePath)
$lit = [System.Drawing.Bitmap]::new($litPath)
$idle = [System.Drawing.Bitmap]::new(128, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$active = [System.Drawing.Bitmap]::new(128, 512, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

try {
    if ($base.Width -ne 128 -or $base.Height -ne 128 -or $lit.Width -ne 128 -or $lit.Height -ne 128) {
        throw "Metal Furnace source textures must both be 128x128"
    }

    # The original base color contains a permanently bright furnace charge. Darken
    # only that chamber for the idle state while preserving the authored metalwork.
    for ($y = 0; $y -lt 128; $y++) {
        for ($x = 0; $x -lt 128; $x++) {
            $pixel = $base.GetPixel($x, $y)
            $inChamber = $x -ge 53 -and $x -le 88 -and $y -ge 16 -and $y -le 54
            $isHot = $pixel.R -gt 125 -and $pixel.G -gt 55 -and $pixel.B -lt 105
            if ($inChamber -and $isHot) {
                $luminance = (0.2126 * $pixel.R) + (0.7152 * $pixel.G) + (0.0722 * $pixel.B)
                $red = [int][Math]::Min(86, 24 + ($luminance * 0.22))
                $green = [int][Math]::Min(43, 13 + ($luminance * 0.10))
                $blue = [int][Math]::Min(27, 9 + ($luminance * 0.055))
                $idle.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $red, $green, $blue))
            } else {
                $idle.SetPixel($x, $y, $pixel)
            }
        }
    }

    # Four deterministic flicker frames. Only warm emissive texels vary, so bolts,
    # panels and the authored silhouette remain stable in the world.
    $strengths = @(0.82, 1.00, 0.88, 0.96)
    for ($frame = 0; $frame -lt 4; $frame++) {
        $strength = [double]$strengths[$frame]
        for ($y = 0; $y -lt 128; $y++) {
            for ($x = 0; $x -lt 128; $x++) {
                $pixel = $lit.GetPixel($x, $y)
                $warm = $pixel.R -gt 135 -and $pixel.R -gt ($pixel.G * 1.16) -and $pixel.G -gt 35
                if ($warm) {
                    $flicker = (((($x * 3) + ($y * 5) + ($frame * 7)) % 11) - 5) * 1.35
                    $red = [int][Math]::Max(0, [Math]::Min(255, ($pixel.R * $strength) + $flicker + 12))
                    $green = [int][Math]::Max(0, [Math]::Min(255, ($pixel.G * $strength) + ($flicker * 0.45)))
                    $blue = [int][Math]::Max(0, [Math]::Min(255, $pixel.B * (0.76 + ($frame * 0.025))))
                    $active.SetPixel($x, $y + ($frame * 128), [System.Drawing.Color]::FromArgb($pixel.A, $red, $green, $blue))
                } else {
                    $active.SetPixel($x, $y + ($frame * 128), $pixel)
                }
            }
        }
    }

    $idle.Save((Join-Path $textureRoot "shaft_furnace_large.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $active.Save((Join-Path $textureRoot "shaft_furnace_large_on.png"), [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $active.Dispose()
    $idle.Dispose()
    $lit.Dispose()
    $base.Dispose()
}

Write-Host "Generated the adapted animated Metal Furnace textures." -ForegroundColor Green
