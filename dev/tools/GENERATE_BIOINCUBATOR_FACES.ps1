param(
    [string]$Project = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path,
    [string]$MinecraftClientExtraJar = ''
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

if ([string]::IsNullOrWhiteSpace($MinecraftClientExtraJar)) {
    $UserProfilePath = [Environment]::GetFolderPath('UserProfile')
    $MinecraftClientExtraJar = Join-Path $UserProfilePath '.gradle\caches\forge_gradle\minecraft_repo\versions\1.20.1\client-extra.jar'
}

if (-not (Test-Path -LiteralPath $MinecraftClientExtraJar)) {
    throw "Minecraft client-extra JAR not found: $MinecraftClientExtraJar"
}

$OutputDirectory = Join-Path $Project 'src\main\resources\assets\domesurvival\textures\gui\bio'
[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null

function Read-ZipBitmap([System.IO.Compression.ZipArchive]$Archive, [string]$EntryName) {
    $Entry = $Archive.GetEntry($EntryName)
    if ($null -eq $Entry) {
        throw "Texture entry not found: $EntryName"
    }

    $EntryStream = $Entry.Open()
    try {
        $Loaded = [System.Drawing.Bitmap]::new($EntryStream)
        try {
            return [System.Drawing.Bitmap]::new($Loaded)
        }
        finally {
            $Loaded.Dispose()
        }
    }
    finally {
        $EntryStream.Dispose()
    }
}

function New-TransparentCanvas {
    $Canvas = [System.Drawing.Bitmap]::new(36, 36, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $Canvas.SetResolution(96, 96)
    return $Canvas
}

function New-PixelGraphics([System.Drawing.Bitmap]$Canvas) {
    $Graphics = [System.Drawing.Graphics]::FromImage($Canvas)
    $Graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    $Graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighSpeed
    $Graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $Graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $Graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $Graphics.Clear([System.Drawing.Color]::Transparent)
    return $Graphics
}

function Draw-PixelRegion(
    [System.Drawing.Graphics]$Graphics,
    [System.Drawing.Bitmap]$Source,
    [int]$SourceX, [int]$SourceY, [int]$SourceWidth, [int]$SourceHeight,
    [int]$TargetX, [int]$TargetY, [int]$TargetWidth, [int]$TargetHeight
) {
    $Target = [System.Drawing.Rectangle]::new($TargetX, $TargetY, $TargetWidth, $TargetHeight)
    $SourceRect = [System.Drawing.Rectangle]::new($SourceX, $SourceY, $SourceWidth, $SourceHeight)
    $Graphics.DrawImage($Source, $Target, $SourceRect, [System.Drawing.GraphicsUnit]::Pixel)
}

function Save-Face([System.Drawing.Bitmap]$Canvas, [string]$Name) {
    $Destination = Join-Path $OutputDirectory ($Name + '.png')
    $Canvas.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output "Generated $Destination"
}

$Archive = [System.IO.Compression.ZipFile]::OpenRead($MinecraftClientExtraJar)
try {
    $Cow = Read-ZipBitmap $Archive 'assets/minecraft/textures/entity/cow/cow.png'
    $Sheep = Read-ZipBitmap $Archive 'assets/minecraft/textures/entity/sheep/sheep.png'
    $Pig = Read-ZipBitmap $Archive 'assets/minecraft/textures/entity/pig/pig.png'
    $Chicken = Read-ZipBitmap $Archive 'assets/minecraft/textures/entity/chicken.png'

    try {
        foreach ($Definition in @(
            @{ Name = 'cow'; Source = $Cow; X = 6; Y = 6; Width = 8; Height = 8; TargetX = 2; TargetY = 2; TargetWidth = 32; TargetHeight = 32 },
            @{ Name = 'sheep'; Source = $Sheep; X = 8; Y = 8; Width = 6; Height = 6; TargetX = 3; TargetY = 3; TargetWidth = 30; TargetHeight = 30 }
        )) {
            $Canvas = New-TransparentCanvas
            $Graphics = New-PixelGraphics $Canvas
            try {
                Draw-PixelRegion $Graphics $Definition.Source $Definition.X $Definition.Y $Definition.Width $Definition.Height $Definition.TargetX $Definition.TargetY $Definition.TargetWidth $Definition.TargetHeight
                Save-Face $Canvas $Definition.Name
            }
            finally {
                $Graphics.Dispose()
                $Canvas.Dispose()
            }
        }

        # Pig head plus the separate vanilla snout cube. The previous icon used
        # only the head face, which made the animal look like a flat pink square.
        $Canvas = New-TransparentCanvas
        $Graphics = New-PixelGraphics $Canvas
        try {
            Draw-PixelRegion $Graphics $Pig 8 8 8 8 2 2 32 32
            Draw-PixelRegion $Graphics $Pig 17 17 4 3 10 18 16 12
            Save-Face $Canvas 'pig'
        }
        finally {
            $Graphics.Dispose()
            $Canvas.Dispose()
        }

        # Chicken uses three vanilla model parts: head, beak and wattle.
        $Canvas = New-TransparentCanvas
        $Graphics = New-PixelGraphics $Canvas
        try {
            Draw-PixelRegion $Graphics $Chicken 3 3 4 6 8 0 20 30
            Draw-PixelRegion $Graphics $Chicken 16 2 4 2 8 15 20 10
            Draw-PixelRegion $Graphics $Chicken 16 6 2 2 13 25 10 10
            Save-Face $Canvas 'chicken'
        }
        finally {
            $Graphics.Dispose()
            $Canvas.Dispose()
        }
    }
    finally {
        $Cow.Dispose()
        $Sheep.Dispose()
        $Pig.Dispose()
        $Chicken.Dispose()
    }
}
finally {
    $Archive.Dispose()
}
