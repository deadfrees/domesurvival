$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$sheet = New-Object Drawing.Bitmap 840,940
$g = [Drawing.Graphics]::FromImage($sheet)
$g.Clear([Drawing.Color]::FromArgb(30,35,39))
$g.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
$font = New-Object Drawing.Font 'Consolas',11
$titleFont = New-Object Drawing.Font 'Consolas',17
$brush = New-Object Drawing.SolidBrush ([Drawing.Color]::FromArgb(195,207,211))
$muted = New-Object Drawing.SolidBrush ([Drawing.Color]::FromArgb(110,130,140))
$g.DrawString('DOMESURVIVAL / PRESS PRODUCTS', $titleFont, $brush, 22,14)
$g.DrawString('32 x 32 sprites | 3x preview + native size', $font, $muted, 22,45)
$types = @('plate','gear','rod','wire','tube')
$materials = @('copper','tin','steel','nickel','silver','lead','goteium','voltarium')
for ($col=0; $col -lt 5; $col++) { $g.DrawString($types[$col],$font,$brush,110+$col*145,76) }
for ($row=0; $row -lt 8; $row++) {
    $g.DrawString($materials[$row],$font,$brush,12,140+$row*100)
    for ($col=0; $col -lt 5; $col++) {
        $path=Join-Path $root ('src/main/resources/assets/domesurvival/textures/item/press_products/'+$materials[$row]+'_'+$types[$col]+'.png')
        if (Test-Path -LiteralPath $path) {
            $icon=[Drawing.Image]::FromFile($path)
            $g.DrawImage($icon,[Drawing.Rectangle]::new(104+$col*145,101+$row*100,96,96),0,0,$icon.Width,$icon.Height,[Drawing.GraphicsUnit]::Pixel)
            $g.DrawImage($icon,[Drawing.Rectangle]::new(202+$col*145,153+$row*100,32,32),0,0,$icon.Width,$icon.Height,[Drawing.GraphicsUnit]::Pixel)
            $icon.Dispose()
        }
    }
}
$out=Join-Path $root 'source_assets/blender/press_products/overview.png'
$sheet.Save($out,[Drawing.Imaging.ImageFormat]::Png)
$g.Dispose();$sheet.Dispose();$font.Dispose();$titleFont.Dispose();$brush.Dispose();$muted.Dispose()
Write-Output $out
