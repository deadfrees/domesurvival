param(
    [string]$Project = "C:\domesurvival",
    [string]$ClientJar = "C:\Users\deadfrees\.gradle\caches\forge_gradle\minecraft_repo\versions\1.20.1\client.jar"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$importRoot = Join-Path $Project "dev\art_reference\blast_furnace_dark_ready_import\src\main\resources\assets\domesurvival"
$textureRoot = Join-Path $Project "src\main\resources\assets\domesurvival\textures\block\shaft_furnace_dark"
$modelRoot = Join-Path $Project "src\main\resources\assets\domesurvival\models"
$utf8NoBom = New-Object Text.UTF8Encoding($false)

function Write-CleanBrickTexture {
    param([string]$SourcePath, [string]$DestinationPath)

    $source = New-Object Drawing.Bitmap($SourcePath)
    try {
        $result = New-Object Drawing.Bitmap($source.Width, $source.Height, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            for ($y = 0; $y -lt $source.Height; $y++) {
                for ($x = 0; $x -lt $source.Width; $x++) {
                    $pixel = $source.GetPixel($x, $y)
                    $luma = (0.2126 * $pixel.R) + (0.7152 * $pixel.G) + (0.0722 * $pixel.B)
                    $neutral = [Math]::Max($pixel.R, [Math]::Max($pixel.G, $pixel.B)) -
                               [Math]::Min($pixel.R, [Math]::Min($pixel.G, $pixel.B)) -le 24
                    if ($pixel.A -gt 0 -and $neutral -and $luma -gt 104) {
                        $detail = [Math]::Min(94, [Math]::Max(58, [int][Math]::Round(58 + (($luma - 104) * 0.22))))
                        $pixel = [Drawing.Color]::FromArgb($pixel.A, $detail - 8, $detail - 3, $detail + 3)
                    }
                    $result.SetPixel($x, $y, $pixel)
                }
            }
            $result.Save($DestinationPath, [Drawing.Imaging.ImageFormat]::Png)
        }
        finally { $result.Dispose() }
    }
    finally { $source.Dispose() }
}

function Write-CleanToolTexture {
    param([string]$SourcePath, [string]$DestinationPath)

    $source = New-Object Drawing.Bitmap($SourcePath)
    try {
        $result = New-Object Drawing.Bitmap($source.Width, $source.Height, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            for ($y = 0; $y -lt $source.Height; $y++) {
                for ($x = 0; $x -lt $source.Width; $x++) {
                    $pixel = $source.GetPixel($x, $y)
                    $luma = (0.2126 * $pixel.R) + (0.7152 * $pixel.G) + (0.0722 * $pixel.B)
                    $neutral = [Math]::Max($pixel.R, [Math]::Max($pixel.G, $pixel.B)) -
                               [Math]::Min($pixel.R, [Math]::Min($pixel.G, $pixel.B)) -le 28
                    if ($pixel.A -gt 0 -and $neutral -and $luma -gt 108) {
                        $pixel = [Drawing.Color]::FromArgb($pixel.A, 78, 84, 90)
                    }
                    $result.SetPixel($x, $y, $pixel)
                }
            }
            $result.Save($DestinationPath, [Drawing.Imaging.ImageFormat]::Png)
        }
        finally { $result.Dispose() }
    }
    finally { $source.Dispose() }
}

foreach ($name in @("bfbricks.png", "bfbricksdark.png", "bfbrickslit.png")) {
    Write-CleanBrickTexture (Join-Path $importRoot "textures\block\$name") (Join-Path $textureRoot $name)
}
Write-CleanToolTexture (Join-Path $importRoot "textures\block\bftoolst.png") (Join-Path $textureRoot "bftoolst.png")

if (-not (Test-Path -LiteralPath $ClientJar)) { throw "Minecraft client JAR was not found: $ClientJar" }
$archive = [IO.Compression.ZipFile]::OpenRead($ClientJar)
try {
    $entry = $archive.GetEntry("assets/minecraft/textures/entity/skeleton/wither_skeleton.png")
    if ($null -eq $entry) { throw "Vanilla wither skeleton texture was not found in $ClientJar" }
    $stream = $entry.Open()
    try {
        $skin = New-Object Drawing.Bitmap($stream)
        try {
            # Keep the original Mojang pixels and atlas layout. The model below
            # only maps the vanilla head UVs onto a smaller display cube.
            $skin.Save((Join-Path $textureRoot "wither_skeleton_head.png"), [Drawing.Imaging.ImageFormat]::Png)
        }
        finally { $skin.Dispose() }
    }
    finally { $stream.Dispose() }
}
finally { $archive.Dispose() }

$skullHead = @'
{
  "name": "wither_skull_head",
  "from": [13.9, 16.08, 2.9],
  "to": [15.65, 17.83, 4.65],
  "rotation": { "angle": -22.5, "axis": "y", "origin": [14.775, 16.955, 3.775] },
  "faces": {
    "north": { "uv": [2, 4, 4, 8], "texture": "#7" },
    "east": { "uv": [0, 4, 2, 8], "texture": "#7" },
    "south": { "uv": [6, 4, 8, 8], "texture": "#7" },
    "west": { "uv": [4, 4, 6, 8], "texture": "#7" },
    "up": { "uv": [2, 0, 4, 4], "texture": "#7" },
    "down": { "uv": [4, 0, 6, 4], "texture": "#7" }
  }
}
'@ | ConvertFrom-Json

foreach ($relativePath in @(
    "block\shaft_furnace_ready.json",
    "block\shaft_furnace_ready_on.json",
    "item\shaft_furnace.json"
)) {
    $path = Join-Path $modelRoot $relativePath
    $model = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    $model.textures."0" = "minecraft:block/polished_deepslate"
    $model.textures.particle = "minecraft:block/polished_deepslate"
    $model.textures | Add-Member -MemberType NoteProperty -Name "7" -Value "domesurvival:block/shaft_furnace_dark/wither_skeleton_head" -Force
    $model.textures.PSObject.Properties.Remove("8")
    $model.elements = @($model.elements | Where-Object {
        $name = [string]$_.name
        $name -notmatch '^firetools_[0-9]+$' -and
        $name -notin @("BFtoolst_0", "BFtoolst_1", "BFtoolst_2", "wither_skull_head", "wither_skull_jaw")
    })
    $model.elements += $skullHead
    [IO.File]::WriteAllText($path, (($model | ConvertTo-Json -Depth 100) + [Environment]::NewLine), $utf8NoBom)
}

Write-Host "Shaft-furnace model and textures polished."
