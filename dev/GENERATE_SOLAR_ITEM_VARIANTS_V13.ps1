$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

Add-Type -AssemblyName System.Drawing

$project = Split-Path -Parent $PSScriptRoot
$textureRoot = Join-Path $project 'src\main\resources\assets\domesurvival\textures\item'
$sourcePath = Join-Path $textureRoot 'solar_generator_mk2.png'
$source = New-Object System.Drawing.Bitmap $sourcePath

function Convert-HsvToColor([double]$h, [double]$s, [double]$v, [int]$alpha) {
    $sector = [Math]::Floor($h * 6.0)
    $fraction = $h * 6.0 - $sector
    $p = $v * (1.0 - $s)
    $q = $v * (1.0 - $fraction * $s)
    $t = $v * (1.0 - (1.0 - $fraction) * $s)
    switch ([int]$sector % 6) {
        0 { $r = $v; $g = $t; $b = $p }
        1 { $r = $q; $g = $v; $b = $p }
        2 { $r = $p; $g = $v; $b = $t }
        3 { $r = $p; $g = $q; $b = $v }
        4 { $r = $t; $g = $p; $b = $v }
        default { $r = $v; $g = $p; $b = $q }
    }
    return [System.Drawing.Color]::FromArgb(
        $alpha,
        [int][Math]::Round($r * 255.0),
        [int][Math]::Round($g * 255.0),
        [int][Math]::Round($b * 255.0)
    )
}

function Write-Variant([string]$fileName, [double]$targetHue) {
    $result = New-Object System.Drawing.Bitmap $source.Width, $source.Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                $pixel = $source.GetPixel($x, $y)
                if ($pixel.A -eq 0) {
                    $result.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
                    continue
                }

                $r = $pixel.R / 255.0
                $g = $pixel.G / 255.0
                $b = $pixel.B / 255.0
                $maximum = [Math]::Max($r, [Math]::Max($g, $b))
                $minimum = [Math]::Min($r, [Math]::Min($g, $b))
                $saturation = if ($maximum -le 0.0) { 0.0 } else { ($maximum - $minimum) / $maximum }

                # Recolour only the photovoltaic cells and their coloured glow.
                # Neutral frame pixels remain byte-for-byte the same shape and tone.
                if ($saturation -ge 0.18 -and $b -gt ($r * 0.78)) {
                    $newSaturation = [Math]::Min(0.92, [Math]::Max(0.56, $saturation))
                    $newValue = [Math]::Min(1.0, $maximum * 1.04)
                    $result.SetPixel($x, $y, (Convert-HsvToColor $targetHue $newSaturation $newValue $pixel.A))
                } else {
                    $result.SetPixel($x, $y, $pixel)
                }
            }
        }
        $destinationName = if ($fileName -eq 'solar_generator_mk2.png') {
            'solar_generator_mk2.generated.png'
        } else {
            $fileName
        }
        $destination = Join-Path $textureRoot $destinationName
        $result.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Host "[OK] $fileName"
    } finally {
        $result.Dispose()
    }
}

try {
    # All tiers share the exact Malachite silhouette; only cell hue changes.
    Write-Variant 'solar_generator_mk1.png' 0.57  # Azurite blue
    Write-Variant 'solar_generator_mk2.png' 0.39  # Malachite green
    Write-Variant 'solar_generator_mk3.png' 0.77  # Amethyst purple
} finally {
    $source.Dispose()
}

Move-Item -LiteralPath (Join-Path $textureRoot 'solar_generator_mk2.generated.png') `
    -Destination (Join-Path $textureRoot 'solar_generator_mk2.png') -Force
