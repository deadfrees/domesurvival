[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$projectPath = (Resolve-Path -LiteralPath $Project).Path
$textureRoot = Join-Path $projectPath "src\main\resources\assets\domesurvival\textures\item\engineering_gears"
if (-not (Test-Path -LiteralPath $textureRoot -PathType Container)) {
    throw "Engineering gear texture directory is missing: $textureRoot"
}

$size = 16
$center = 7.5
$materialPalettes = [ordered]@{
    steel = @("#20262D", "#39434D", "#52606C", "#6B7A86", "#84939E")
    tin = @("#2F3C4E", "#526A80", "#7894A8", "#91AABC", "#AAC1CF")
    lead = @("#211F2B", "#393546", "#565062", "#6B6578", "#817A8E")
    nickel = @("#34342F", "#56544B", "#7D796B", "#989282", "#B1AA98")
}

function Test-GearPixel {
    param([int]$X, [int]$Y)

    $dx = ($X + 0.5) - $center
    $dy = ($Y + 0.5) - $center
    $radius = [Math]::Sqrt($dx * $dx + $dy * $dy)
    if ($radius -le 4.9) {
        return $true
    }
    if ($radius -gt 6.9) {
        return $false
    }

    $sector = [Math]::PI / 4.0
    $angle = [Math]::Atan2($dy, $dx)
    $nearestTooth = [Math]::Round($angle / $sector) * $sector
    $angleFromToothCenter = [Math]::Abs($angle - $nearestTooth)
    return $angleFromToothCenter -le 0.22
}

$mask = [bool[]]::new($size * $size)
for ($y = 0; $y -lt $size; $y++) {
    for ($x = 0; $x -lt $size; $x++) {
        $mask[$y * $size + $x] = Test-GearPixel -X $x -Y $y
    }
}

function Test-OutlinePixel {
    param([int]$X, [int]$Y)

    foreach ($point in @(
        @(($X - 1), $Y),
        @(($X + 1), $Y),
        @($X, ($Y - 1)),
        @($X, ($Y + 1))
    )) {
        $nx = $point[0]
        $ny = $point[1]
        if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $size -or $ny -ge $size -or
            -not $mask[$ny * $size + $nx]) {
            return $true
        }
    }
    return $false
}

function Get-ShadeIndex {
    param([int]$X, [int]$Y)

    $dx = ($X + 0.5) - $center
    $dy = ($Y + 0.5) - $center
    $radius = [Math]::Sqrt($dx * $dx + $dy * $dy)

    if ($radius -le 1.45) {
        return 0
    }
    if ($radius -le 2.55) {
        return 1
    }
    if (Test-OutlinePixel -X $X -Y $Y) {
        return 0
    }

    $lightBand = $X + $Y
    if ($lightBand -le 10) { return 4 }
    if ($lightBand -le 14) { return 3 }
    if ($lightBand -le 18) { return 2 }
    return 1
}

function Bleed-TransparentEdgeColor {
    param([System.Drawing.Bitmap]$Bitmap, [int]$Passes = 2)

    $pixelCount = $Bitmap.Width * $Bitmap.Height
    $red = [int[]]::new($pixelCount)
    $green = [int[]]::new($pixelCount)
    $blue = [int[]]::new($pixelCount)
    $colored = [bool[]]::new($pixelCount)

    for ($y = 0; $y -lt $Bitmap.Height; $y++) {
        for ($x = 0; $x -lt $Bitmap.Width; $x++) {
            $index = $y * $Bitmap.Width + $x
            $pixel = $Bitmap.GetPixel($x, $y)
            $red[$index] = $pixel.R
            $green[$index] = $pixel.G
            $blue[$index] = $pixel.B
            $colored[$index] = $pixel.A -eq 255
        }
    }

    for ($pass = 0; $pass -lt $Passes; $pass++) {
        $nextRed = [int[]]$red.Clone()
        $nextGreen = [int[]]$green.Clone()
        $nextBlue = [int[]]$blue.Clone()
        $nextColored = [bool[]]$colored.Clone()

        for ($y = 0; $y -lt $Bitmap.Height; $y++) {
            for ($x = 0; $x -lt $Bitmap.Width; $x++) {
                $index = $y * $Bitmap.Width + $x
                if ($colored[$index]) { continue }

                $sumRed = 0
                $sumGreen = 0
                $sumBlue = 0
                $neighbors = 0
                foreach ($point in @(
                    @(($x - 1), $y),
                    @(($x + 1), $y),
                    @($x, ($y - 1)),
                    @($x, ($y + 1))
                )) {
                    $nx = $point[0]
                    $ny = $point[1]
                    if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $Bitmap.Width -or $ny -ge $Bitmap.Height) {
                        continue
                    }
                    $neighborIndex = $ny * $Bitmap.Width + $nx
                    if (-not $colored[$neighborIndex]) { continue }
                    $sumRed += $red[$neighborIndex]
                    $sumGreen += $green[$neighborIndex]
                    $sumBlue += $blue[$neighborIndex]
                    $neighbors++
                }

                if ($neighbors -gt 0) {
                    $nextRed[$index] = [int][Math]::Round($sumRed / $neighbors)
                    $nextGreen[$index] = [int][Math]::Round($sumGreen / $neighbors)
                    $nextBlue[$index] = [int][Math]::Round($sumBlue / $neighbors)
                    $nextColored[$index] = $true
                }
            }
        }

        $red = $nextRed
        $green = $nextGreen
        $blue = $nextBlue
        $colored = $nextColored
    }

    for ($y = 0; $y -lt $Bitmap.Height; $y++) {
        for ($x = 0; $x -lt $Bitmap.Width; $x++) {
            $index = $y * $Bitmap.Width + $x
            if ($Bitmap.GetPixel($x, $y).A -eq 0) {
                $Bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(
                    0, $red[$index], $green[$index], $blue[$index]
                ))
            }
        }
    }
}

foreach ($material in $materialPalettes.Keys) {
    $palette = @($materialPalettes[$material] | ForEach-Object {
        [System.Drawing.ColorTranslator]::FromHtml($_)
    })
    $bitmap = [System.Drawing.Bitmap]::new(
        $size,
        $size,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    try {
        for ($y = 0; $y -lt $size; $y++) {
            for ($x = 0; $x -lt $size; $x++) {
                if (-not $mask[$y * $size + $x]) {
                    $bitmap.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
                    continue
                }
                $shade = Get-ShadeIndex -X $x -Y $y
                $target = $palette[$shade]
                $bitmap.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(
                    255, $target.R, $target.G, $target.B
                ))
            }
        }

        Bleed-TransparentEdgeColor -Bitmap $bitmap
        $destination = Join-Path $textureRoot ($material + "_gear.png")
        $temporary = $destination + ".simple.png"
        $bitmap.Save($temporary, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }

    Move-Item -LiteralPath $temporary -Destination $destination -Force
    Write-Host "Generated simple 16x16 $material gear."
}
