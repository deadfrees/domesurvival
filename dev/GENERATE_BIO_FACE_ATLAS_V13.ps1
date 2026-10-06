$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

Add-Type -AssemblyName System.Drawing

$project = Split-Path -Parent $PSScriptRoot
$target = Join-Path $project 'src\main\resources\assets\domesurvival\textures\gui\bio\bio_faces_atlas.png'
$bitmap = New-Object System.Drawing.Bitmap 160, 128, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.Clear([System.Drawing.Color]::Transparent)
$graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor

function Color([string]$hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function Rect([int]$column, [int]$row, [int]$x, [int]$y, [int]$width, [int]$height, [string]$hex) {
    $brush = New-Object System.Drawing.SolidBrush (Color $hex)
    try {
        $graphics.FillRectangle($brush, $column * 32 + $x, $row * 32 + $y, $width, $height)
    } finally {
        $brush.Dispose()
    }
}

# Rabbit
Rect 0 0 6 1 7 13 '#887467'; Rect 0 0 19 1 7 13 '#887467'
Rect 0 0 8 3 3 9 '#d58d9e'; Rect 0 0 21 3 3 9 '#d58d9e'
Rect 0 0 4 11 24 18 '#78695f'; Rect 0 0 8 18 4 5 '#201c1b'; Rect 0 0 20 18 4 5 '#201c1b'
Rect 0 0 10 23 12 6 '#b8aaa0'; Rect 0 0 15 22 3 3 '#e9a0ad'

# Horse
Rect 1 0 7 2 6 8 '#6f381d'; Rect 1 0 19 2 6 8 '#6f381d'; Rect 1 0 5 7 22 22 '#88451f'
Rect 1 0 14 7 4 15 '#eee5d7'; Rect 1 0 8 14 4 5 '#171411'; Rect 1 0 20 14 4 5 '#171411'
Rect 1 0 8 22 16 8 '#44352d'; Rect 1 0 11 25 3 3 '#171514'; Rect 1 0 18 25 3 3 '#171514'

# Donkey
Rect 2 0 5 1 7 13 '#776d68'; Rect 2 0 20 1 7 13 '#776d68'; Rect 2 0 7 4 3 8 '#a79d93'; Rect 2 0 22 4 3 8 '#a79d93'
Rect 2 0 5 10 22 19 '#77706b'; Rect 2 0 8 16 4 5 '#161515'; Rect 2 0 20 16 4 5 '#161515'
Rect 2 0 8 22 16 7 '#b5aca5'; Rect 2 0 11 25 3 3 '#282423'; Rect 2 0 18 25 3 3 '#282423'

# Llama
Rect 3 0 7 2 6 9 '#d8c6a2'; Rect 3 0 19 2 6 9 '#d8c6a2'; Rect 3 0 5 8 22 21 '#dcc9a5'
Rect 3 0 8 15 4 5 '#211e19'; Rect 3 0 20 15 4 5 '#211e19'; Rect 3 0 9 21 14 8 '#b49a76'
Rect 3 0 12 24 3 3 '#554438'; Rect 3 0 18 24 3 3 '#554438'

# Goat
Rect 4 0 4 1 6 11 '#5d5b5c'; Rect 4 0 22 1 6 11 '#5d5b5c'; Rect 4 0 2 9 7 5 '#d9d6ce'; Rect 4 0 23 9 7 5 '#d9d6ce'
Rect 4 0 5 7 22 22 '#edeae1'; Rect 4 0 8 15 4 5 '#181818'; Rect 4 0 20 15 4 5 '#181818'
Rect 4 0 10 22 12 7 '#b9b2a8'; Rect 4 0 15 25 3 4 '#383434'

# Camel
Rect 0 1 2 7 7 5 '#c89245'; Rect 0 1 23 7 7 5 '#c89245'; Rect 0 1 6 5 20 24 '#c9954c'
Rect 0 1 8 14 4 5 '#211b14'; Rect 0 1 20 14 4 5 '#211b14'; Rect 0 1 8 21 16 8 '#9d713d'
Rect 0 1 12 24 3 3 '#3b2b20'; Rect 0 1 18 24 3 3 '#3b2b20'

# Wolf
Rect 1 1 5 3 7 10 '#777b7d'; Rect 1 1 20 3 7 10 '#777b7d'; Rect 1 1 7 6 3 6 '#b5b6b3'; Rect 1 1 22 6 3 6 '#b5b6b3'
Rect 1 1 5 10 22 18 '#8b8f90'; Rect 1 1 8 16 4 5 '#171819'; Rect 1 1 20 16 4 5 '#171819'
Rect 1 1 9 21 14 8 '#d1cfca'; Rect 1 1 14 22 5 4 '#242526'; Rect 1 1 12 27 8 3 '#4d4f50'

# Cat (calico)
Rect 2 1 4 4 8 10 '#3b3c3d'; Rect 2 1 20 4 8 10 '#b96c24'; Rect 2 1 5 10 22 18 '#dfd5c8'
Rect 2 1 5 10 8 10 '#47494a'; Rect 2 1 21 10 6 11 '#cf7928'; Rect 2 1 8 16 4 5 '#56a83f'; Rect 2 1 20 16 4 5 '#56a83f'
Rect 2 1 14 21 4 3 '#d77b91'; Rect 2 1 10 24 5 2 '#665b54'; Rect 2 1 18 24 5 2 '#665b54'

# Ocelot
Rect 3 1 5 4 7 9 '#c88725'; Rect 3 1 20 4 7 9 '#c88725'; Rect 3 1 5 10 22 18 '#d9a13a'
Rect 3 1 8 14 3 3 '#72491c'; Rect 3 1 21 14 3 3 '#72491c'; Rect 3 1 8 17 4 5 '#258a40'; Rect 3 1 20 17 4 5 '#258a40'
Rect 3 1 14 22 4 3 '#9d4b4d'; Rect 3 1 11 25 10 3 '#e1c27c'

# Fox
Rect 4 1 4 3 8 11 '#d65e19'; Rect 4 1 20 3 8 11 '#d65e19'; Rect 4 1 6 5 4 6 '#2b211e'; Rect 4 1 22 5 4 6 '#2b211e'
Rect 4 1 4 10 24 18 '#e56c1c'; Rect 4 1 6 18 6 6 '#f1eee5'; Rect 4 1 20 18 6 6 '#f1eee5'
Rect 4 1 8 15 4 5 '#171516'; Rect 4 1 20 15 4 5 '#171516'; Rect 4 1 10 22 12 7 '#f3eee2'; Rect 4 1 14 22 5 4 '#201b19'

# Bee
Rect 0 2 5 6 22 21 '#e5ae22'; Rect 0 2 3 9 4 11 '#24201b'; Rect 0 2 25 9 4 11 '#24201b'
Rect 0 2 8 6 4 5 '#24201b'; Rect 0 2 20 6 4 5 '#24201b'; Rect 0 2 6 14 20 4 '#37271a'
Rect 0 2 8 18 5 6 '#66cde2'; Rect 0 2 19 18 5 6 '#66cde2'; Rect 0 2 14 22 4 3 '#5b3518'

# Panda
Rect 1 2 4 4 8 9 '#272727'; Rect 1 2 20 4 8 9 '#272727'; Rect 1 2 5 8 22 20 '#ecebe4'
Rect 1 2 7 14 7 7 '#303030'; Rect 1 2 18 14 7 7 '#303030'; Rect 1 2 9 16 3 3 '#f1f1ec'; Rect 1 2 20 16 3 3 '#f1f1ec'
Rect 1 2 12 21 8 6 '#d4d0c8'; Rect 1 2 14 21 4 3 '#262626'; Rect 1 2 15 24 2 3 '#4b4541'

# Turtle
Rect 2 2 4 7 24 20 '#237f42'; Rect 2 2 7 5 18 4 '#1e6d39'; Rect 2 2 7 14 4 5 '#152d20'; Rect 2 2 21 14 4 5 '#152d20'
Rect 2 2 8 22 16 6 '#80a657'; Rect 2 2 13 21 6 3 '#3d6538'

# Axolotl
Rect 3 2 7 7 18 21 '#ef93b0'; Rect 3 2 2 8 6 4 '#d94e8a'; Rect 3 2 1 14 7 4 '#d94e8a'; Rect 3 2 3 20 5 4 '#d94e8a'
Rect 3 2 24 8 6 4 '#d94e8a'; Rect 3 2 24 14 7 4 '#d94e8a'; Rect 3 2 24 20 5 4 '#d94e8a'
Rect 3 2 10 15 4 5 '#35243c'; Rect 3 2 20 15 4 5 '#35243c'; Rect 3 2 14 21 5 3 '#c95d82'

# Frog
Rect 4 2 5 7 22 20 '#52983e'; Rect 4 2 5 5 8 7 '#64ae4c'; Rect 4 2 19 5 8 7 '#64ae4c'
Rect 4 2 7 7 4 5 '#171a13'; Rect 4 2 21 7 4 5 '#171a13'; Rect 4 2 7 19 18 7 '#c7b76e'
Rect 4 2 12 20 8 2 '#4d7134'; Rect 4 2 14 24 4 2 '#816d43'

# Mooshroom
Rect 0 3 3 4 9 8 '#cf2723'; Rect 0 3 20 4 9 8 '#cf2723'; Rect 0 3 5 8 22 20 '#bd2926'
Rect 0 3 7 10 5 5 '#f3ebe0'; Rect 0 3 19 18 6 5 '#f3ebe0'; Rect 0 3 8 17 4 5 '#191515'; Rect 0 3 20 17 4 5 '#191515'
Rect 0 3 9 22 14 6 '#d8cbc0'; Rect 0 3 13 24 3 3 '#38302e'; Rect 0 3 18 24 3 3 '#38302e'

# Sniffer
Rect 1 3 4 6 24 12 '#236d5c'; Rect 1 3 6 4 20 5 '#2e826c'; Rect 1 3 4 16 24 12 '#8d3a31'
Rect 1 3 7 17 4 5 '#252323'; Rect 1 3 21 17 4 5 '#252323'; Rect 1 3 8 21 16 8 '#d5a523'
Rect 1 3 11 23 3 4 '#3c2a1b'; Rect 1 3 18 23 3 4 '#3c2a1b'; Rect 1 3 5 9 5 3 '#3d9475'; Rect 1 3 21 8 5 3 '#3d9475'

# Strider
Rect 2 3 4 6 24 21 '#a63c3a'; Rect 2 3 6 5 20 4 '#b54845'; Rect 2 3 7 15 5 5 '#f1eee1'; Rect 2 3 20 15 5 5 '#f1eee1'
Rect 2 3 9 16 3 3 '#272120'; Rect 2 3 20 16 3 3 '#272120'; Rect 2 3 9 23 14 2 '#642a2a'
Rect 2 3 6 27 3 4 '#8c3434'; Rect 2 3 13 27 3 4 '#8c3434'; Rect 2 3 21 27 3 4 '#8c3434'

# Hoglin
Rect 3 3 3 7 26 20 '#9b5a42'; Rect 3 3 5 5 8 6 '#694437'; Rect 3 3 20 5 8 6 '#694437'
Rect 3 3 7 14 4 5 '#f0eee4'; Rect 3 3 21 14 4 5 '#f0eee4'; Rect 3 3 5 20 6 8 '#d9c26e'; Rect 3 3 21 20 6 8 '#d9c26e'
Rect 3 3 10 20 12 8 '#5e3b35'; Rect 3 3 12 23 3 4 '#241d1c'; Rect 3 3 18 23 3 4 '#241d1c'

try {
    $bitmap.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    $graphics.Dispose()
    $bitmap.Dispose()
}

Write-Host "[OK] Clean 160x128 bio face atlas written: $target"
