param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$assetRoot = Join-Path $Project "src\main\resources\assets\domesurvival"
$modelRoot = Join-Path $assetRoot "models\block"
$sourceModelRoot = Join-Path $Project "dev\art_reference\bioincubator_model_original"
$iconRoot = Join-Path $assetRoot "textures\gui\bio"
$referenceRoot = Join-Path $Project "dev\art_reference\bio_gui_full_animals"
$utf8NoBom = New-Object Text.UTF8Encoding($false)

function Write-JsonFile {
    param([string]$Path, [object]$Value)
    [IO.File]::WriteAllText($Path, (($Value | ConvertTo-Json -Depth 100) + [Environment]::NewLine), $utf8NoBom)
}

function Has-Texture {
    param([object]$Element, [string]$Texture)
    return @($Element.faces.PSObject.Properties.Value.texture) -contains $Texture
}

function Set-AxisRange {
    param([object]$Element, [int]$Axis, [double]$From, [double]$To)
    $Element.from[$Axis] = $From
    $Element.to[$Axis] = $To
}

function Move-Axis {
    param([object]$Element, [int]$Axis, [double]$Offset)
    $Element.from[$Axis] = [Math]::Round(([double]$Element.from[$Axis] + $Offset), 3)
    $Element.to[$Axis] = [Math]::Round(([double]$Element.to[$Axis] + $Offset), 3)
}

function Remove-EdgeBackground {
    param([Drawing.Bitmap]$Bitmap)

    $width = $Bitmap.Width
    $height = $Bitmap.Height
    $visited = New-Object 'bool[,]' $width, $height
    $queue = [Collections.Generic.Queue[Drawing.Point]]::new()
    for ($x = 0; $x -lt $width; $x++) {
        $queue.Enqueue([Drawing.Point]::new($x, 0))
        $queue.Enqueue([Drawing.Point]::new($x, $height - 1))
    }
    for ($y = 1; $y -lt $height - 1; $y++) {
        $queue.Enqueue([Drawing.Point]::new(0, $y))
        $queue.Enqueue([Drawing.Point]::new($width - 1, $y))
    }

    while ($queue.Count -gt 0) {
        $point = $queue.Dequeue()
        if ($visited[$point.X, $point.Y]) { continue }
        $visited[$point.X, $point.Y] = $true
        $pixel = $Bitmap.GetPixel($point.X, $point.Y)
        $spread = [Math]::Max($pixel.R, [Math]::Max($pixel.G, $pixel.B)) -
                  [Math]::Min($pixel.R, [Math]::Min($pixel.G, $pixel.B))
        $background = $pixel.A -eq 0 -or
                      ($pixel.R -ge 205 -and $pixel.G -ge 205 -and $pixel.B -ge 205 -and $spread -le 32)
        if (-not $background) { continue }

        $Bitmap.SetPixel($point.X, $point.Y, [Drawing.Color]::Transparent)
        if ($point.X -gt 0) { $queue.Enqueue([Drawing.Point]::new($point.X - 1, $point.Y)) }
        if ($point.X + 1 -lt $width) { $queue.Enqueue([Drawing.Point]::new($point.X + 1, $point.Y)) }
        if ($point.Y -gt 0) { $queue.Enqueue([Drawing.Point]::new($point.X, $point.Y - 1)) }
        if ($point.Y + 1 -lt $height) { $queue.Enqueue([Drawing.Point]::new($point.X, $point.Y + 1)) }
    }
}

function Polish-BlockModel {
    param([string]$SourcePath, [string]$DestinationPath)

    $model = Get-Content -LiteralPath $SourcePath -Raw | ConvertFrom-Json

    foreach ($element in $model.elements) {
        # The preserved source is the last approved model, where the former
        # 0.10/15.90 perimeter was already clamped once. Restore that neutral
        # baseline before applying one shared outward offset per side.
        for ($axis = 0; $axis -lt 3; $axis++) {
            if ([Math]::Abs([double]$element.from[$axis] - 0.02) -lt 0.0001) {
                $element.from[$axis] = 0.10
            }
            if ([Math]::Abs([double]$element.to[$axis] - 15.98) -lt 0.0001) {
                $element.to[$axis] = 15.90
            }
        }

        $isPad = Has-Texture $element "#pad"
        $isCyan = Has-Texture $element "#cyan"
        $smallConnector = $false

        if ($isPad) {
            if ([double]$element.from[1] -gt 15.0) { Set-AxisRange $element 1 15.70 15.945 }
            elseif ([double]$element.to[1] -lt 1.0) { Set-AxisRange $element 1 0.035 0.30 }
            elseif ([double]$element.to[0] -lt 2.0) { Set-AxisRange $element 0 0.055 0.30 }
            elseif ([double]$element.from[0] -gt 14.0) { Set-AxisRange $element 0 15.70 15.945 }
            elseif ([double]$element.from[2] -gt 14.0) { Set-AxisRange $element 2 15.70 15.945 }
        }

        if ($isCyan) {
            $sizeX = [double]$element.to[0] - [double]$element.from[0]
            $sizeY = [double]$element.to[1] - [double]$element.from[1]
            $sizeZ = [double]$element.to[2] - [double]$element.from[2]
            $smallConnector = $sizeX -le 1.5 -and $sizeY -le 1.5 -and $sizeZ -le 1.5
            if ($smallConnector) {
                if ([double]$element.from[1] -gt 15.0) { Set-AxisRange $element 1 15.950 15.965 }
                elseif ([double]$element.to[0] -lt 1.0) { Set-AxisRange $element 0 0.035 0.050 }
                elseif ([double]$element.from[0] -gt 15.0) { Set-AxisRange $element 0 15.950 15.965 }
                elseif ([double]$element.from[2] -gt 15.0) { Set-AxisRange $element 2 15.950 15.965 }
            }
        }

        # Bring every visible outer skin to the block perimeter. Connector
        # backing/face layers keep their explicitly separated depths above.
        # Each side uses one shared offset, preserving the original depth
        # differences instead of collapsing decorative layers onto one plane.
        if (-not $isPad -and -not ($isCyan -and $smallConnector)) {
            if ([double]$element.to[0] -le 1.75) {
                Move-Axis $element 0 -0.08
            }
            elseif ([double]$element.from[0] -ge 14.25) {
                Move-Axis $element 0 0.08
            }

            if ([double]$element.to[2] -le 1.05) {
                Move-Axis $element 2 -0.08
            }
            elseif ([double]$element.from[2] -ge 14.0) {
                Move-Axis $element 2 0.08
            }

            if ([double]$element.from[1] -ge 14.8) {
                Move-Axis $element 1 0.08
            }
            elseif ([double]$element.from[1] -gt 0.0 -and [double]$element.to[1] -le 0.9) {
                Move-Axis $element 1 -0.08
            }
        }
    }

    Write-JsonFile $DestinationPath $model
}

foreach ($name in @("bioincubator.json", "bioincubator_lit.json")) {
    Polish-BlockModel (Join-Path $sourceModelRoot $name) (Join-Path $modelRoot $name)
}

# Create inset, bioincubator-only dynamic port faces. They touch the backing
# layers visually, remain inside 0..16 and keep separate depths to avoid flicker.
$ranges = @{
    up    = @{ axis = 1; from = 15.970; to = 15.990 }
    down  = @{ axis = 1; from = 0.010;  to = 0.030 }
    north = @{ axis = 2; from = 0.010;  to = 0.030 }
    south = @{ axis = 2; from = 15.970; to = 15.990 }
    west  = @{ axis = 0; from = 0.010;  to = 0.030 }
    east  = @{ axis = 0; from = 15.970; to = 15.990 }
}

foreach ($mode in @("input", "output")) {
    foreach ($direction in @("up", "down", "north", "south", "west", "east")) {
        $source = Join-Path $modelRoot "machine_${mode}_port_${direction}.json"
        $model = Get-Content -LiteralPath $source -Raw | ConvertFrom-Json
        $range = $ranges[$direction]
        Set-AxisRange $model.elements[0] $range.axis $range.from $range.to
        Write-JsonFile (Join-Path $modelRoot "bioincubator_${mode}_port_${direction}.json") $model
    }
}

# Crop the user-approved front-facing portrait sheet into uniform 36x36 GUI
# icons. Pig is prepared for the damaged capsule but is not enabled in logic.
[IO.Directory]::CreateDirectory($referenceRoot) | Out-Null
$faceSheetPath = Join-Path $referenceRoot "bio_animal_faces_reference.png"
if (-not (Test-Path -LiteralPath $faceSheetPath)) {
    throw "Missing animal face reference: $faceSheetPath"
}
$crops = @{
    chicken = [Drawing.Rectangle]::new(125, 16, 86, 83)
    cow     = [Drawing.Rectangle]::new(226, 16, 85, 83)
    pig     = [Drawing.Rectangle]::new(426, 16, 93, 83)
    sheep   = [Drawing.Rectangle]::new(531, 16, 96, 83)
}

$source = New-Object Drawing.Bitmap($faceSheetPath)
try {
    foreach ($name in @("chicken", "sheep", "cow", "pig")) {
        $iconPath = Join-Path $iconRoot "$name.png"
        $result = New-Object Drawing.Bitmap(36, 36, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [Drawing.Graphics]::FromImage($result)
            try {
                $graphics.Clear([Drawing.Color]::Transparent)
                $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
                $graphics.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.DrawImage($source, [Drawing.Rectangle]::new(2, 2, 32, 32), $crops[$name], [Drawing.GraphicsUnit]::Pixel)
            }
            finally { $graphics.Dispose() }

            if ($name -eq "chicken") {
                # The chicken's actual face is white and merges with the sheet
                # background, so use its blocky head/wattle silhouette.
                for ($y = 0; $y -lt $result.Height; $y++) {
                    for ($x = 0; $x -lt $result.Width; $x++) {
                        $head = $x -ge 3 -and $x -le 32 -and $y -ge 3 -and $y -le 27
                        $wattle = $x -ge 12 -and $x -le 23 -and $y -ge 28 -and $y -le 35
                        if (-not $head -and -not $wattle) {
                            $result.SetPixel($x, $y, [Drawing.Color]::Transparent)
                        }
                    }
                }
            }
            else {
                Remove-EdgeBackground $result
            }
            $result.Save($iconPath, [Drawing.Imaging.ImageFormat]::Png)
        }
        finally { $result.Dispose() }
    }
}
finally { $source.Dispose() }

Write-Host "Bioincubator model, ports and face icons polished."
