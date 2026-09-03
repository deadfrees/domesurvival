param(
    [string]$Project = "C:\domesurvival",
    [string]$ClientJar = "C:\Users\deadfrees\.gradle\caches\forge_gradle\minecraft_repo\versions\1.20.1\client.jar"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$textureRoot = Join-Path $Project "src\main\resources\assets\domesurvival\textures\block\shaft_furnace_dark"
if (-not (Test-Path -LiteralPath $textureRoot)) {
    throw "Shaft-furnace texture directory was not found: $textureRoot"
}
if (-not (Test-Path -LiteralPath $ClientJar)) {
    throw "Minecraft 1.20.1 client JAR was not found: $ClientJar"
}

function Convert-HsvToColor {
    param([double]$Hue, [double]$Saturation, [double]$Value, [int]$Alpha)

    $chroma = $Value * $Saturation
    $section = ($Hue / 60.0) % 6.0
    $x = $chroma * (1.0 - [Math]::Abs(($section % 2.0) - 1.0))
    $r1 = 0.0; $g1 = 0.0; $b1 = 0.0
    if ($section -lt 1.0) { $r1 = $chroma; $g1 = $x }
    elseif ($section -lt 2.0) { $r1 = $x; $g1 = $chroma }
    elseif ($section -lt 3.0) { $g1 = $chroma; $b1 = $x }
    elseif ($section -lt 4.0) { $g1 = $x; $b1 = $chroma }
    elseif ($section -lt 5.0) { $r1 = $x; $b1 = $chroma }
    else { $r1 = $chroma; $b1 = $x }

    $m = $Value - $chroma
    $red = [Math]::Min(255, [Math]::Max(0, [int][Math]::Round(($r1 + $m) * 255.0)))
    $green = [Math]::Min(255, [Math]::Max(0, [int][Math]::Round(($g1 + $m) * 255.0)))
    $blue = [Math]::Min(255, [Math]::Max(0, [int][Math]::Round(($b1 + $m) * 255.0)))
    return [Drawing.Color]::FromArgb($Alpha, $red, $green, $blue)
}

function Convert-WarmPixelsToBlue {
    param([Drawing.Bitmap]$Source)

    $result = New-Object Drawing.Bitmap($Source.Width, $Source.Height, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt $Source.Height; $y++) {
        for ($x = 0; $x -lt $Source.Width; $x++) {
            $pixel = $Source.GetPixel($x, $y)
            if ($pixel.A -eq 0) {
                $result.SetPixel($x, $y, $pixel)
                continue
            }

            $r = $pixel.R / 255.0
            $g = $pixel.G / 255.0
            $b = $pixel.B / 255.0
            $max = [Math]::Max($r, [Math]::Max($g, $b))
            $min = [Math]::Min($r, [Math]::Min($g, $b))
            $delta = $max - $min
            $saturation = if ($max -le 0.0) { 0.0 } else { $delta / $max }
            $hue = 0.0
            if ($delta -gt 0.0) {
                if ($max -eq $r) { $hue = 60.0 * ((($g - $b) / $delta) % 6.0) }
                elseif ($max -eq $g) { $hue = 60.0 * ((($b - $r) / $delta) + 2.0) }
                else { $hue = 60.0 * ((($r - $g) / $delta) + 4.0) }
                if ($hue -lt 0.0) { $hue += 360.0 }
            }

            $isWarmGlow = $max -ge 0.22 -and $saturation -ge 0.24 -and (($hue -le 78.0) -or ($hue -ge 345.0))
            if (-not $isWarmGlow) {
                $result.SetPixel($x, $y, $pixel)
                continue
            }

            $blueHue = 224.0 - (46.0 * $max)
            $blueSaturation = if ($max -ge 0.82) { 0.62 } elseif ($max -ge 0.58) { 0.82 } else { 0.96 }
            $blueValue = [Math]::Min(1.0, 0.06 + ($max * 1.06))
            $result.SetPixel($x, $y, (Convert-HsvToColor $blueHue $blueSaturation $blueValue $pixel.A))
        }
    }
    return $result
}

function Convert-AllVisiblePixelsToBlue {
    param([Drawing.Bitmap]$Source)

    $result = New-Object Drawing.Bitmap($Source.Width, $Source.Height, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt $Source.Height; $y++) {
        for ($x = 0; $x -lt $Source.Width; $x++) {
            $pixel = $Source.GetPixel($x, $y)
            if ($pixel.A -eq 0) {
                $result.SetPixel($x, $y, $pixel)
                continue
            }
            $value = [Math]::Max($pixel.R, [Math]::Max($pixel.G, $pixel.B)) / 255.0
            $blueHue = 224.0 - (46.0 * $value)
            $blueSaturation = if ($value -ge 0.82) { 0.62 } elseif ($value -ge 0.58) { 0.82 } else { 0.96 }
            $blueValue = [Math]::Min(1.0, 0.06 + ($value * 1.06))
            $result.SetPixel($x, $y, (Convert-HsvToColor $blueHue $blueSaturation $blueValue $pixel.A))
        }
    }
    return $result
}

function Convert-TextureFile {
    param([string]$SourcePath, [string]$DestinationPath)

    $source = New-Object Drawing.Bitmap($SourcePath)
    try {
        $converted = Convert-WarmPixelsToBlue $source
        try { $converted.Save($DestinationPath, [Drawing.Imaging.ImageFormat]::Png) }
        finally { $converted.Dispose() }
    }
    finally { $source.Dispose() }
}

Convert-TextureFile (Join-Path $textureRoot "bftoolshot.png") (Join-Path $textureRoot "bftoolshot_blue.png")
Convert-TextureFile (Join-Path $textureRoot "campfire_log_lit.png") (Join-Path $textureRoot "campfire_log_lit_blue.png")
Convert-TextureFile (Join-Path $textureRoot "firetools.png") (Join-Path $textureRoot "firetools_blue.png")

$archive = [IO.Compression.ZipFile]::OpenRead($ClientJar)
try {
    $entry = $archive.GetEntry("assets/minecraft/textures/block/fire_0.png")
    if ($null -eq $entry) { throw "Vanilla fire_0.png was not found in $ClientJar" }
    $stream = $entry.Open()
    try {
        $source = New-Object Drawing.Bitmap($stream)
        try {
            $converted = Convert-AllVisiblePixelsToBlue $source
            try { $converted.Save((Join-Path $textureRoot "blue_fire.png"), [Drawing.Imaging.ImageFormat]::Png) }
            finally { $converted.Dispose() }
        }
        finally { $source.Dispose() }
    }
    finally { $stream.Dispose() }
}
finally { $archive.Dispose() }

Write-Host "Blue shaft-furnace fire textures generated."
