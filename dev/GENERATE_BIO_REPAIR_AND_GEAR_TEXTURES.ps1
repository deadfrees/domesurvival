param(
    [Parameter(Mandatory = $true)]
    [string]$RepairKitSource,
    [string]$SieveMeshSource = ''
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$textureRoot = Join-Path $projectRoot 'src\main\resources\assets\domesurvival\textures\item'
$repairOutput = Join-Path $textureRoot 'genetics\bio_repair_kit.png'
$meshOutput = Join-Path $textureRoot 'sieve_mesh.png'

function Save-NearestNeighborResize {
    param(
        [System.Drawing.Bitmap]$Source,
        [int]$Width,
        [int]$Height,
        [string]$Destination
    )

    $target = New-Object System.Drawing.Bitmap($Width, $Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $target.SetResolution(96, 96)
        $graphics = [System.Drawing.Graphics]::FromImage($target)
        try {
            $graphics.Clear([System.Drawing.Color]::Transparent)
            $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
            $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighSpeed
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
            $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
            $graphics.DrawImage($Source, [System.Drawing.Rectangle]::new(0, 0, $Width, $Height))
        }
        finally {
            $graphics.Dispose()
        }
        $target.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $target.Dispose()
    }
}

function Save-CroppedRepairKit {
    param([string]$SourcePath, [string]$Destination)

    $source = [System.Drawing.Bitmap]::FromFile((Resolve-Path -LiteralPath $SourcePath))
    try {
        $minX = $source.Width
        $minY = $source.Height
        $maxX = -1
        $maxY = -1
        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                if ($source.GetPixel($x, $y).A -gt 8) {
                    if ($x -lt $minX) { $minX = $x }
                    if ($y -lt $minY) { $minY = $y }
                    if ($x -gt $maxX) { $maxX = $x }
                    if ($y -gt $maxY) { $maxY = $y }
                }
            }
        }
        if ($maxX -lt $minX -or $maxY -lt $minY) {
            throw 'Generated repair-kit image contains no visible pixels.'
        }

        $contentWidth = $maxX - $minX + 1
        $contentHeight = $maxY - $minY + 1
        $side = [Math]::Max($contentWidth, $contentHeight)
        $square = New-Object System.Drawing.Bitmap($side, $side, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($square)
            try {
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $offsetX = [int](($side - $contentWidth) / 2)
                $offsetY = [int](($side - $contentHeight) / 2)
                $sourceRect = [System.Drawing.Rectangle]::new($minX, $minY, $contentWidth, $contentHeight)
                $destinationRect = [System.Drawing.Rectangle]::new($offsetX, $offsetY, $contentWidth, $contentHeight)
                $graphics.DrawImage($source, $destinationRect, $sourceRect, [System.Drawing.GraphicsUnit]::Pixel)
            }
            finally {
                $graphics.Dispose()
            }

            $inner = New-Object System.Drawing.Bitmap(28, 28, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            try {
                $graphics = [System.Drawing.Graphics]::FromImage($inner)
                try {
                    $graphics.Clear([System.Drawing.Color]::Transparent)
                    $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
                    $graphics.DrawImage($square, [System.Drawing.Rectangle]::new(0, 0, 28, 28))
                }
                finally {
                    $graphics.Dispose()
                }

                $result = New-Object System.Drawing.Bitmap(32, 32, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
                try {
                    $graphics = [System.Drawing.Graphics]::FromImage($result)
                    try {
                        $graphics.Clear([System.Drawing.Color]::Transparent)
                        $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                        $graphics.DrawImageUnscaled($inner, 2, 2)
                    }
                    finally {
                        $graphics.Dispose()
                    }
                    $result.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
                }
                finally {
                    $result.Dispose()
                }
            }
            finally {
                $inner.Dispose()
            }
        }
        finally {
            $square.Dispose()
        }
    }
    finally {
        $source.Dispose()
    }
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $repairOutput) | Out-Null
Save-CroppedRepairKit -SourcePath $RepairKitSource -Destination $repairOutput
if (-not [string]::IsNullOrWhiteSpace($SieveMeshSource)) {
    Save-CroppedRepairKit -SourcePath $SieveMeshSource -Destination $meshOutput
}

$gearRoot = Join-Path $textureRoot 'engineering_gears'
Get-ChildItem -LiteralPath $gearRoot -Filter '*.png' -File | ForEach-Object {
    $source = [System.Drawing.Bitmap]::FromFile($_.FullName)
    try {
        if ($source.Width -eq 16 -and $source.Height -eq 16) {
            $temporary = $_.FullName + '.32.png'
            Save-NearestNeighborResize -Source $source -Width 32 -Height 32 -Destination $temporary
            $source.Dispose()
            Move-Item -LiteralPath $temporary -Destination $_.FullName -Force
        }
    }
    finally {
        $source.Dispose()
    }
}

Write-Output "Generated project item textures and normalized engineering gears to 32x32."
