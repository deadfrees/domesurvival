[CmdletBinding()]
param([string]$Project = "C:\domesurvival")

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$projectPath = (Resolve-Path -LiteralPath $Project).Path
$blockRoot = Join-Path $projectPath "src\main\resources\assets\domesurvival\textures\block\metallurgy"
$itemRoot = Join-Path $projectPath "src\main\resources\assets\domesurvival\textures\item\metallurgy"
New-Item -ItemType Directory -Force -Path $blockRoot, $itemRoot | Out-Null

function Color([string]$hex) {
    if ($hex -eq "#00000000") { return [System.Drawing.Color]::FromArgb(0, 0, 0, 0) }
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function New-Canvas([int]$width, [int]$height, [string]$fill) {
    $bitmap = [System.Drawing.Bitmap]::new($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try { $graphics.Clear((Color $fill)) } finally { $graphics.Dispose() }
    return $bitmap
}

function Rect($bitmap, [int]$x, [int]$y, [int]$width, [int]$height, [string]$hex) {
    for ($py = $y; $py -lt $y + $height; $py++) {
        for ($px = $x; $px -lt $x + $width; $px++) {
            if ($px -ge 0 -and $px -lt $bitmap.Width -and $py -ge 0 -and $py -lt $bitmap.Height) {
                $bitmap.SetPixel($px, $py, (Color $hex))
            }
        }
    }
}

function Pixel($bitmap, [int]$x, [int]$y, [string]$hex) {
    if ($x -ge 0 -and $x -lt $bitmap.Width -and $y -ge 0 -and $y -lt $bitmap.Height) {
        $bitmap.SetPixel($x, $y, (Color $hex))
    }
}

function Polygon($bitmap, [object[]]$points, [string]$hex) {
    $drawingPoints = @($points | ForEach-Object { [System.Drawing.Point]::new([int]$_[0], [int]$_[1]) })
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $brush = [System.Drawing.SolidBrush]::new((Color $hex))
    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::None
        $graphics.FillPolygon($brush, $drawingPoints)
    } finally {
        $brush.Dispose()
        $graphics.Dispose()
    }
}

function Save-Png($bitmap, [string]$path) {
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

function Draw-MetalPlate($bitmap, [int]$offsetY = 0) {
    Rect $bitmap 0 $offsetY 16 16 "#202529"
    Rect $bitmap 1 ($offsetY + 1) 14 14 "#30383D"
    Rect $bitmap 2 ($offsetY + 2) 12 2 "#465159"
    Rect $bitmap 2 ($offsetY + 4) 12 3 "#353E44"
    Rect $bitmap 2 ($offsetY + 9) 12 4 "#2B3237"
    Rect $bitmap 2 ($offsetY + 13) 12 1 "#171C1F"
    Rect $bitmap 0 ($offsetY + 7) 16 2 "#171B1E"
    Pixel $bitmap 1 ($offsetY + 1) "#647078"
    Pixel $bitmap 14 ($offsetY + 1) "#121619"
    Pixel $bitmap 1 ($offsetY + 14) "#121619"
    Pixel $bitmap 14 ($offsetY + 14) "#566169"
    Pixel $bitmap 5 ($offsetY + 5) "#3E484E"
    Pixel $bitmap 10 ($offsetY + 11) "#242B2F"
}

function Draw-Brick($bitmap) {
    Rect $bitmap 0 0 16 16 "#4C2D24"
    for ($y = 0; $y -lt 16; $y += 4) {
        Rect $bitmap 0 $y 16 1 "#2A2220"
        $shift = if ((($y / 4) % 2) -eq 0) { 0 } else { 4 }
        for ($x = $shift; $x -lt 16; $x += 8) { Rect $bitmap $x $y 1 4 "#342620" }
    }
    Rect $bitmap 1 1 6 1 "#74412F"
    Rect $bitmap 9 5 5 1 "#68392D"
    Rect $bitmap 2 9 5 1 "#75412F"
    Rect $bitmap 10 13 4 1 "#61352A"
    Pixel $bitmap 6 2 "#3B2823"
    Pixel $bitmap 12 10 "#3D2924"
}

function Draw-CokeFront($bitmap, [int]$offsetY, [string]$glow) {
    Rect $bitmap 0 $offsetY 16 16 "#4C2D24"
    Rect $bitmap 0 $offsetY 16 2 "#252A2E"
    Rect $bitmap 0 ($offsetY + 13) 16 3 "#202529"
    Rect $bitmap 1 ($offsetY + 2) 2 11 "#333A3F"
    Rect $bitmap 13 ($offsetY + 2) 2 11 "#333A3F"
    Rect $bitmap 3 ($offsetY + 3) 10 9 "#141719"
    Rect $bitmap 4 ($offsetY + 4) 8 7 "#252A2D"
    for ($x = 5; $x -le 10; $x += 2) { Rect $bitmap $x ($offsetY + 5) 1 5 $glow }
    Rect $bitmap 4 ($offsetY + 10) 8 1 "#111315"
    Pixel $bitmap 1 ($offsetY + 1) "#697178"
    Pixel $bitmap 14 ($offsetY + 1) "#15181A"
    Pixel $bitmap 3 ($offsetY + 3) "#626D74"
    Pixel $bitmap 12 ($offsetY + 3) "#181C1F"
    Pixel $bitmap 3 ($offsetY + 11) "#171B1E"
    Pixel $bitmap 12 ($offsetY + 11) "#505A61"
}

function Draw-ShaftFront($bitmap, [int]$offsetY, [string]$glow) {
    Draw-MetalPlate $bitmap $offsetY
    Rect $bitmap 2 ($offsetY + 3) 10 10 "#171A1D"
    Rect $bitmap 3 ($offsetY + 4) 8 8 "#2B3034"
    Rect $bitmap 4 ($offsetY + 5) 6 6 "#111315"
    for ($x = 4; $x -le 9; $x += 2) { Rect $bitmap $x ($offsetY + 6) 1 5 $glow }
    Rect $bitmap 12 ($offsetY + 5) 3 6 "#171A1C"
    Rect $bitmap 13 ($offsetY + 6) 1 4 $glow
    Pixel $bitmap 2 ($offsetY + 3) "#647078"
    Pixel $bitmap 11 ($offsetY + 3) "#15191C"
    Rect $bitmap 3 ($offsetY + 12) 8 1 "#1A1F22"
}

function Draw-Connector($bitmap, [string]$rim, [string]$inner) {
    Rect $bitmap 0 0 16 16 "#171B1E"
    Rect $bitmap 1 1 14 14 "#333B40"
    Rect $bitmap 3 3 10 10 $rim
    Rect $bitmap 5 5 6 6 "#111416"
    Rect $bitmap 6 6 4 4 $inner
    Pixel $bitmap 2 2 "#5A646B"
    Pixel $bitmap 13 13 "#111416"
}

function Draw-WorldFireFrame($bitmap, [int]$offsetY, [int]$phase, [string]$outer, [string]$middle, [string]$hot) {
    Rect $bitmap 0 $offsetY 16 16 "#090505"
    Rect $bitmap 2 ($offsetY + 3) 12 10 "#170705"
    Rect $bitmap 2 ($offsetY + 12) 12 1 "#351008"
    $heightSets = @(
        @(5, 8, 6, 9),
        @(7, 5, 9, 6),
        @(6, 9, 5, 8),
        @(8, 6, 7, 5)
    )
    $heights = $heightSets[$phase]
    for ($flame = 0; $flame -lt 4; $flame++) {
        $x = 3 + ($flame * 3)
        $bottom = $offsetY + 13
        $height = $heights[$flame]
        Rect $bitmap $x ($bottom - $height) 2 $height $outer
        Rect $bitmap ($x + 1) ($bottom - $height + 2) 1 ($height - 2) $middle
        if ($height -ge 7) { Pixel $bitmap $x ($bottom - $height - 1) $middle }
        Pixel $bitmap ($x + 1) ($bottom - 3) $hot
        if ((($phase + $flame) % 2) -eq 0) { Pixel $bitmap $x ($bottom - 1) $hot }
    }
}

function Draw-WorldIdlePanel($bitmap, [string]$recess, [string]$slat, [string]$ember) {
    Rect $bitmap 0 0 16 16 "#080A0B"
    Rect $bitmap 1 2 14 12 "#2B3236"
    Rect $bitmap 2 3 12 10 "#111517"
    Rect $bitmap 3 3 10 9 $recess
    Rect $bitmap 3 3 10 1 $slat
    Rect $bitmap 3 6 10 1 "#090B0C"
    Rect $bitmap 3 9 10 1 "#07090A"
    Rect $bitmap 7 4 1 2 "#0B0D0E"
    Rect $bitmap 5 7 1 2 "#25292B"
    Rect $bitmap 10 7 1 2 "#0A0C0D"
    Rect $bitmap 3 10 10 2 "#0A0908"
    Pixel $bitmap 4 4 "#596165"
    Pixel $bitmap 11 5 "#050607"
    Pixel $bitmap 4 10 $ember
    Pixel $bitmap 7 11 $ember
    Pixel $bitmap 10 10 $ember
    Pixel $bitmap 12 11 "#25100A"
    Rect $bitmap 2 12 12 1 "#030405"
}

function Draw-WorldGrate($bitmap, [int]$offsetY) {
    # A texture grille cannot protrude or pick up shader-colored side highlights.
    # Four bars form two mirrored pairs around the center of the chamber.
    Rect $bitmap 2 ($offsetY + 3) 12 1 "#5D666A"
    Rect $bitmap 2 ($offsetY + 12) 12 1 "#171C1F"
    foreach ($x in @(3, 6, 9, 12)) {
        Rect $bitmap $x ($offsetY + 3) 1 10 "#3E474B"
        Pixel $bitmap $x ($offsetY + 3) "#788185"
        Pixel $bitmap $x ($offsetY + 12) "#151A1D"
    }
}

$brick = New-Canvas 16 16 "#000000"
Draw-Brick $brick
Save-Png $brick (Join-Path $blockRoot "coke_oven_brick.png")

$metal = New-Canvas 16 16 "#000000"
Draw-MetalPlate $metal
Save-Png $metal (Join-Path $blockRoot "dark_metal.png")

$cokeOff = New-Canvas 16 16 "#000000"
Draw-CokeFront $cokeOff 0 "#1A1715"
Save-Png $cokeOff (Join-Path $blockRoot "coke_oven_front.png")

$cokeOn = New-Canvas 16 64 "#000000"
$glows = @("#8A3210", "#C74B10", "#F07816", "#D95A11")
for ($frame = 0; $frame -lt 4; $frame++) { Draw-CokeFront $cokeOn ($frame * 16) $glows[$frame] }
Save-Png $cokeOn (Join-Path $blockRoot "coke_oven_front_on.png")

$upper = New-Canvas 16 16 "#000000"
Draw-Brick $upper
Rect $upper 3 4 10 7 "#202427"
Rect $upper 4 5 8 5 "#111416"
for ($x = 5; $x -le 10; $x += 2) { Rect $upper $x 6 1 3 "#3B4145" }
Rect $upper 11 11 3 3 "#202427"
Pixel $upper 12 12 "#9D4215"
Save-Png $upper (Join-Path $blockRoot "coke_oven_upper_front.png")

$shaftSide = New-Canvas 16 16 "#000000"
Draw-MetalPlate $shaftSide
Rect $shaftSide 3 3 10 10 "#30363B"
Rect $shaftSide 4 4 8 1 "#4B545A"
Rect $shaftSide 4 11 8 1 "#1D2225"
Rect $shaftSide 7 3 2 10 "#22272B"
Save-Png $shaftSide (Join-Path $blockRoot "shaft_furnace_side.png")

$shaftOff = New-Canvas 16 16 "#000000"
Draw-ShaftFront $shaftOff 0 "#18191A"
Save-Png $shaftOff (Join-Path $blockRoot "shaft_furnace_front.png")

$shaftOn = New-Canvas 16 64 "#000000"
for ($frame = 0; $frame -lt 4; $frame++) { Draw-ShaftFront $shaftOn ($frame * 16) $glows[$frame] }
Save-Png $shaftOn (Join-Path $blockRoot "shaft_furnace_front_on.png")

$shaftUpper = New-Canvas 16 16 "#000000"
Draw-MetalPlate $shaftUpper
Rect $shaftUpper 3 3 10 6 "#15191C"
for ($x = 4; $x -le 11; $x += 2) { Rect $shaftUpper $x 4 1 4 "#3C454B" }
Rect $shaftUpper 4 10 8 3 "#22272B"
Pixel $shaftUpper 11 11 "#A64316"
Save-Png $shaftUpper (Join-Path $blockRoot "shaft_furnace_upper_front.png")

$inputConnector = New-Canvas 16 16 "#000000"
Draw-Connector $inputConnector "#8A561E" "#B36D22"
Save-Png $inputConnector (Join-Path $blockRoot "input_connector.png")

$outputConnector = New-Canvas 16 16 "#000000"
Draw-Connector $outputConnector "#526F7D" "#6F94A5"
Save-Png $outputConnector (Join-Path $blockRoot "output_connector.png")

$slagItem = New-Canvas 16 16 "#00000000"
Polygon $slagItem @(@(8,2),@(12,3),@(13,6),@(11,8),@(12,11),@(8,14),@(4,12),@(5,9),@(2,7),@(4,4)) "#1B1F22"
Polygon $slagItem @(@(8,3),@(11,4),@(11,6),@(9,8),@(10,10),@(7,12),@(5,11),@(6,8),@(4,7),@(5,5)) "#42474A"
Polygon $slagItem @(@(8,3),@(10,4),@(8,6),@(5,7),@(5,5)) "#6A6E6F"
Pixel $slagItem 7 9 "#704127"; Pixel $slagItem 9 7 "#583624"; Pixel $slagItem 6 11 "#2A2F32"
Save-Png $slagItem (Join-Path $itemRoot "slag.png")

function Recolor-Texture(
    [string]$sourcePath,
    [string]$destinationPath,
    [int[]]$shadow,
    [int[]]$mid,
    [int[]]$highlight
) {
    $source = [System.Drawing.Bitmap]::new($sourcePath)
    $result = [System.Drawing.Bitmap]::new(
        $source.Width,
        $source.Height,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    try {
        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                $pixel = $source.GetPixel($x, $y)
                if ($pixel.A -eq 0) {
                    $result.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
                    continue
                }
                $luminance = (0.2126 * $pixel.R + 0.7152 * $pixel.G + 0.0722 * $pixel.B) / 255.0
                $luminance = [Math]::Max(0.0, [Math]::Min(1.0, (($luminance - 0.5) * 1.18) + 0.5))
                if ($luminance -lt 0.5) {
                    $from = $shadow; $to = $mid; $amount = $luminance * 2.0
                } else {
                    $from = $mid; $to = $highlight; $amount = ($luminance - 0.5) * 2.0
                }
                $red = [int][Math]::Round($from[0] + (($to[0] - $from[0]) * $amount))
                $green = [int][Math]::Round($from[1] + (($to[1] - $from[1]) * $amount))
                $blue = [int][Math]::Round($from[2] + (($to[2] - $from[2]) * $amount))
                $result.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $red, $green, $blue))
            }
        }
        $result.Save($destinationPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $source.Dispose()
        $result.Dispose()
    }
}

function Recolor-Zip-Texture(
    [string]$archivePath,
    [string]$entryPath,
    [string]$destinationPath,
    [int[]]$shadow,
    [int[]]$mid,
    [int[]]$highlight
) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($archivePath)
    $stream = $null
    $source = $null
    $result = $null
    try {
        $entry = $archive.GetEntry($entryPath)
        if ($null -eq $entry) { throw "Missing vanilla texture in archive: $entryPath" }
        $stream = $entry.Open()
        $source = [System.Drawing.Bitmap]::new($stream)
        $result = [System.Drawing.Bitmap]::new(
            $source.Width,
            $source.Height,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )

        $luminances = [System.Collections.Generic.List[double]]::new()
        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                $pixel = $source.GetPixel($x, $y)
                if ($pixel.A -gt 0) {
                    $luminances.Add((0.2126 * $pixel.R + 0.7152 * $pixel.G + 0.0722 * $pixel.B) / 255.0)
                }
            }
        }
        $minimum = ($luminances | Measure-Object -Minimum).Minimum
        $maximum = ($luminances | Measure-Object -Maximum).Maximum
        $range = [Math]::Max(0.001, $maximum - $minimum)

        for ($y = 0; $y -lt $source.Height; $y++) {
            for ($x = 0; $x -lt $source.Width; $x++) {
                $pixel = $source.GetPixel($x, $y)
                if ($pixel.A -eq 0) {
                    $result.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
                    continue
                }
                $raw = (0.2126 * $pixel.R + 0.7152 * $pixel.G + 0.0722 * $pixel.B) / 255.0
                $luminance = [Math]::Max(0.0, [Math]::Min(1.0, ($raw - $minimum) / $range))
                if ($luminance -lt 0.5) {
                    $from = $shadow; $to = $mid; $amount = $luminance * 2.0
                } else {
                    $from = $mid; $to = $highlight; $amount = ($luminance - 0.5) * 2.0
                }
                $red = [int][Math]::Round($from[0] + (($to[0] - $from[0]) * $amount))
                $green = [int][Math]::Round($from[1] + (($to[1] - $from[1]) * $amount))
                $blue = [int][Math]::Round($from[2] + (($to[2] - $from[2]) * $amount))
                $result.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, $red, $green, $blue))
            }
        }
        $result.Save($destinationPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        if ($null -ne $result) { $result.Dispose() }
        if ($null -ne $source) { $source.Dispose() }
        if ($null -ne $stream) { $stream.Dispose() }
        $archive.Dispose()
    }
}

$vanillaClientJar = Join-Path $env:USERPROFILE ".gradle\caches\forge_gradle\minecraft_repo\versions\1.20.1\client-extra.jar"
if (-not (Test-Path -LiteralPath $vanillaClientJar)) {
    throw "Minecraft 1.20.1 client-extra.jar is required to preserve vanilla item silhouettes"
}
Recolor-Zip-Texture $vanillaClientJar "assets/minecraft/textures/item/coal.png" `
    (Join-Path $itemRoot "coal_coke.png") @(52, 57, 60) @(113, 119, 122) @(184, 189, 191)
$customSteelTexture = Join-Path $projectPath "dev\art_reference\steel_ingot.png"
if (-not (Test-Path -LiteralPath $customSteelTexture -PathType Leaf)) {
    throw "Custom steel ingot texture is missing: $customSteelTexture"
}
Copy-Item -LiteralPath $customSteelTexture -Destination (Join-Path $itemRoot "steel_ingot.png") -Force

$copperRoot = Join-Path $projectPath "src\main\resources\assets\domesurvival\textures\block\copper_furnace"
$detailedRoot = Join-Path $blockRoot "detailed"
New-Item -ItemType Directory -Force -Path $detailedRoot | Out-Null

$palettes = @{
    coke_oven = @{
        body = @(@(25, 15, 12), @(100, 52, 34), @(154, 84, 55))
        structure = @(@(12, 16, 18), @(52, 60, 65), @(112, 123, 130))
        iron = @(@(19, 23, 25), @(68, 76, 81), @(139, 149, 154))
        dark = @(@(6, 8, 9), @(29, 34, 37), @(73, 81, 86))
        gauge = @(@(20, 13, 8), @(94, 52, 24), @(171, 101, 43))
        fire = @(@(48, 8, 2), @(224, 55, 5), @(255, 156, 24))
    }
    shaft_furnace = @{
        body = @(@(17, 20, 22), @(70, 77, 82), @(120, 130, 136))
        structure = @(@(12, 16, 18), @(55, 63, 68), @(122, 133, 139))
        iron = @(@(20, 24, 27), @(72, 81, 86), @(148, 157, 162))
        dark = @(@(6, 8, 9), @(27, 32, 35), @(70, 78, 83))
        gauge = @(@(17, 20, 21), @(73, 86, 92), @(131, 151, 159))
        fire = @(@(42, 6, 2), @(204, 43, 4), @(255, 119, 15))
    }
}

foreach ($machine in @("coke_oven", "shaft_furnace")) {
    $palette = $palettes[$machine]
    $rules = @{
        body = @{ source = "body"; colors = $palette.body }
        top = @{ source = "top"; colors = $palette.structure }
        bottom = @{ source = "bottom"; colors = $palette.structure }
        back = @{ source = "back"; colors = $palette.body }
        rim = @{ source = "rim"; colors = $palette.structure }
        iron = @{ source = "iron"; colors = $palette.iron }
        door = @{ source = "door"; colors = $palette.dark }
        fire_off = @{ source = "fire_off"; colors = $palette.dark }
        gauge = @{ source = "gauge"; colors = $palette.gauge }
        vent = @{ source = "vent"; colors = $palette.dark }
        soot = @{ source = "soot"; colors = $palette.dark }
    }
    foreach ($targetName in $rules.Keys) {
        $sourceName = $rules[$targetName].source
        $colors = $rules[$targetName].colors
        Recolor-Texture (Join-Path $copperRoot "copper_furnace_${sourceName}.png") (Join-Path $detailedRoot "${machine}_${targetName}.png") $colors[0] $colors[1] $colors[2]
    }
    $fireCanvas = New-Canvas 16 64 "#090505"
    if ($machine -eq "coke_oven") {
        $outer = "#922307"; $middle = "#E9580B"; $hot = "#FF8A18"
    } else {
        $outer = "#791606"; $middle = "#D83C08"; $hot = "#FF7412"
    }
    for ($frame = 0; $frame -lt 4; $frame++) {
        Draw-WorldFireFrame $fireCanvas ($frame * 16) $frame $outer $middle $hot
        Draw-WorldGrate $fireCanvas ($frame * 16)
    }
    Save-Png $fireCanvas (Join-Path $detailedRoot "${machine}_fire.png")
    $idleCanvas = New-Canvas 16 16 "#080A0B"
    if ($machine -eq "coke_oven") {
        Draw-WorldIdlePanel $idleCanvas "#2B1D18" "#514039" "#A83B12"
    } else {
        Draw-WorldIdlePanel $idleCanvas "#1B2225" "#414B50" "#8F2C0E"
    }
    Draw-WorldGrate $idleCanvas 0
    Save-Png $idleCanvas (Join-Path $detailedRoot "${machine}_fire_off.png")
    Copy-Item -LiteralPath (Join-Path $copperRoot "copper_furnace_fire.png.mcmeta") -Destination (Join-Path $detailedRoot "${machine}_fire.png.mcmeta") -Force
}

# The supplied large furnace has its original static flame cards removed during
# GLB conversion. These frames sit behind the model's own front bars instead.
$largeFireCanvas = New-Canvas 16 64 "#080505"
for ($frame = 0; $frame -lt 4; $frame++) {
    Draw-WorldFireFrame $largeFireCanvas ($frame * 16) $frame "#721305" "#DF4108" "#FF9B1B"
}
Save-Png $largeFireCanvas (Join-Path $blockRoot "large_coke_oven_fire.png")
Copy-Item -LiteralPath (Join-Path $copperRoot "copper_furnace_fire.png.mcmeta") `
    -Destination (Join-Path $blockRoot "large_coke_oven_fire.png.mcmeta") -Force

$largeIdleCanvas = New-Canvas 16 16 "#08090A"
Draw-WorldIdlePanel $largeIdleCanvas "#151719" "#363A3D" "#6D2B14"
Save-Png $largeIdleCanvas (Join-Path $blockRoot "large_coke_oven_fire_off.png")

# Connector plates deliberately stay close to the surrounding steel palette. Their
# role is readable from placement, without the bright side-port colors of the concept.
Recolor-Texture (Join-Path $copperRoot "copper_furnace_top.png") (Join-Path $detailedRoot "input_connector.png") @(10, 13, 15) @(47, 55, 59) @(92, 102, 107)
Recolor-Texture (Join-Path $copperRoot "copper_furnace_bottom.png") (Join-Path $detailedRoot "output_connector.png") @(10, 14, 16) @(43, 57, 63) @(82, 100, 108)

& (Join-Path $projectPath "dev\tools\GENERATE_DETAILED_METALLURGY_MODELS.ps1") -Project $projectPath
& (Join-Path $projectPath "dev\tools\GENERATE_SHAFT_FURNACE_ASSETS.ps1") -Project $projectPath

Write-Host "Generated detailed copper-furnace-based metallurgy assets." -ForegroundColor Green
