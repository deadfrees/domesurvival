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

function Save-Png($bitmap, [string]$path) {
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

function Draw-MetalPlate($bitmap, [int]$offsetY = 0) {
    Rect $bitmap 0 $offsetY 16 16 "#24292D"
    Rect $bitmap 1 ($offsetY + 1) 14 14 "#343B40"
    Rect $bitmap 2 ($offsetY + 2) 12 2 "#485157"
    Rect $bitmap 2 ($offsetY + 13) 12 1 "#202529"
    Rect $bitmap 0 ($offsetY + 7) 16 2 "#1C2023"
    Pixel $bitmap 1 ($offsetY + 1) "#687177"
    Pixel $bitmap 14 ($offsetY + 1) "#16191C"
    Pixel $bitmap 1 ($offsetY + 14) "#16191C"
    Pixel $bitmap 14 ($offsetY + 14) "#596269"
}

function Draw-Brick($bitmap) {
    Rect $bitmap 0 0 16 16 "#593225"
    for ($y = 0; $y -lt 16; $y += 4) {
        Rect $bitmap 0 $y 16 1 "#2A2220"
        $shift = if ((($y / 4) % 2) -eq 0) { 0 } else { 4 }
        for ($x = $shift; $x -lt 16; $x += 8) { Rect $bitmap $x $y 1 4 "#342620" }
    }
    Rect $bitmap 1 1 6 1 "#7A4632"
    Rect $bitmap 9 5 5 1 "#71402F"
    Rect $bitmap 2 9 5 1 "#804934"
    Rect $bitmap 10 13 4 1 "#6E3C2D"
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
}

function Draw-ShaftFront($bitmap, [int]$offsetY, [string]$glow) {
    Draw-MetalPlate $bitmap $offsetY
    Rect $bitmap 2 ($offsetY + 3) 10 10 "#171A1D"
    Rect $bitmap 3 ($offsetY + 4) 8 8 "#2B3034"
    Rect $bitmap 4 ($offsetY + 5) 6 6 "#111315"
    for ($x = 4; $x -le 9; $x += 2) { Rect $bitmap $x ($offsetY + 6) 1 5 $glow }
    Rect $bitmap 12 ($offsetY + 5) 3 6 "#171A1C"
    Rect $bitmap 13 ($offsetY + 6) 1 4 $glow
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

$cokeItem = New-Canvas 16 16 "#00000000"
Rect $cokeItem 4 2 7 2 "#2D3032"
Rect $cokeItem 2 4 11 7 "#17191B"
Rect $cokeItem 4 11 8 3 "#202326"
Pixel $cokeItem 3 5 "#45494C"; Pixel $cokeItem 9 3 "#4A4D50"; Pixel $cokeItem 11 9 "#34383B"
Save-Png $cokeItem (Join-Path $itemRoot "coal_coke.png")

$steelItem = New-Canvas 16 16 "#00000000"
Rect $steelItem 3 4 10 7 "#323A40"
Rect $steelItem 4 3 8 1 "#68747B"
Rect $steelItem 2 6 12 4 "#59656C"
Rect $steelItem 4 10 8 2 "#2A3136"
Rect $steelItem 4 5 7 1 "#849097"
Pixel $steelItem 12 7 "#20262A"
Save-Png $steelItem (Join-Path $itemRoot "steel_ingot.png")

$slagItem = New-Canvas 16 16 "#00000000"
Rect $slagItem 5 2 6 2 "#44484A"
Rect $slagItem 3 4 10 8 "#25292C"
Rect $slagItem 5 12 7 2 "#1B1E20"
Pixel $slagItem 4 5 "#5A5147"; Pixel $slagItem 9 4 "#575B5D"; Pixel $slagItem 11 8 "#6B3A22"
Pixel $slagItem 6 10 "#3D4245"; Pixel $slagItem 8 7 "#784223"
Save-Png $slagItem (Join-Path $itemRoot "slag.png")

Write-Host "Generated canonical metallurgy textures and animated strips." -ForegroundColor Green
