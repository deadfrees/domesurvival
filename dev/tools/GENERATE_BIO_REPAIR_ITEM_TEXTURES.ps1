param(
    [string]$Project = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path,
    [int]$TextureSize = 32
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$SourceDirectory = Join-Path $Project 'dev\art_reference\bio_repair_items'
$OutputDirectory = Join-Path $Project 'src\main\resources\assets\domesurvival\textures\item\genetics'
[IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null

function Convert-ToMinecraftSprite([string]$SourcePath, [string]$DestinationPath) {
    $source = [Drawing.Bitmap]::new($SourcePath)
    try {
        $minX = $source.Width
        $minY = $source.Height
        $maxX = -1
        $maxY = -1

        for ($y = 0; $y -lt $source.Height; $y += 2) {
            for ($x = 0; $x -lt $source.Width; $x += 2) {
                if ($source.GetPixel($x, $y).A -gt 16) {
                    $minX = [Math]::Min($minX, $x)
                    $minY = [Math]::Min($minY, $y)
                    $maxX = [Math]::Max($maxX, $x)
                    $maxY = [Math]::Max($maxY, $y)
                }
            }
        }

        if ($maxX -lt $minX -or $maxY -lt $minY) {
            throw "No visible pixels in $SourcePath"
        }

        $width = $maxX - $minX + 1
        $height = $maxY - $minY + 1
        $side = [Math]::Max($width, $height)
        $cropX = [Math]::Max(0, [int][Math]::Floor($minX - ($side - $width) / 2.0))
        $cropY = [Math]::Max(0, [int][Math]::Floor($minY - ($side - $height) / 2.0))
        $side = [Math]::Min($side, [Math]::Min($source.Width - $cropX, $source.Height - $cropY))

        $margin = [Math]::Max(1, [int]($TextureSize / 16))
        $contentSize = $TextureSize - $margin * 2
        $sprite = [Drawing.Bitmap]::new($TextureSize, $TextureSize, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [Drawing.Graphics]::FromImage($sprite)
            try {
                $graphics.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
                $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::None
                $graphics.Clear([Drawing.Color]::Transparent)
                $graphics.DrawImage(
                    $source,
                    [Drawing.Rectangle]::new($margin, $margin, $contentSize, $contentSize),
                    [Drawing.Rectangle]::new($cropX, $cropY, $side, $side),
                    [Drawing.GraphicsUnit]::Pixel
                )
            }
            finally {
                $graphics.Dispose()
            }

            # Eliminate semitransparent HD edge pixels and keep the palette compact.
            for ($y = 0; $y -lt $TextureSize; $y++) {
                for ($x = 0; $x -lt $TextureSize; $x++) {
                    $pixel = $sprite.GetPixel($x, $y)
                    if ($pixel.A -lt 128) {
                        $sprite.SetPixel($x, $y, [Drawing.Color]::Transparent)
                        continue
                    }
                    $r = [Math]::Min(255, [int]([Math]::Round($pixel.R / 16.0) * 16))
                    $g = [Math]::Min(255, [int]([Math]::Round($pixel.G / 16.0) * 16))
                    $b = [Math]::Min(255, [int]([Math]::Round($pixel.B / 16.0) * 16))
                    $sprite.SetPixel($x, $y, [Drawing.Color]::FromArgb(255, $r, $g, $b))
                }
            }

            $sprite.Save($DestinationPath, [Drawing.Imaging.ImageFormat]::Png)
        }
        finally {
            $sprite.Dispose()
        }
    }
    finally {
        $source.Dispose()
    }
}

Convert-ToMinecraftSprite `
    (Join-Path $SourceDirectory 'biogel_source.png') `
    (Join-Path $OutputDirectory 'biogel.png')
Convert-ToMinecraftSprite `
    (Join-Path $SourceDirectory 'nutrient_mix_source.png') `
    (Join-Path $OutputDirectory 'nutrient_mix.png')

Write-Output "Generated ${TextureSize}x${TextureSize} Minecraft-style biogel and nutrient mixture sprites."
