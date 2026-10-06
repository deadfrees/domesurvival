$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$manifest=Get-Content (Join-Path $PSScriptRoot 'manifest.json') -Raw | ConvertFrom-Json
$font=New-Object Drawing.Font 'Consolas',10
$brush=New-Object Drawing.SolidBrush ([Drawing.Color]::FromArgb(207,214,215))
function Sheet($entries,$columns,$cellW,$cellH,$name) {
    $rows=[int][Math]::Ceiling($entries.Count/$columns)
    $bitmap=New-Object Drawing.Bitmap ($columns*$cellW),($rows*$cellH+40)
    $g=[Drawing.Graphics]::FromImage($bitmap);$g.Clear([Drawing.Color]::FromArgb(31,37,40))
    $g.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $g.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
    $g.DrawString('DOMESURVIVAL / '+$name,$font,$brush,12,10)
    for($i=0;$i -lt $entries.Count;$i++) {
        $entry=$entries[$i];$x=($i%$columns)*$cellW;$y=[int][Math]::Floor($i/$columns)*$cellH+40
        $icon=[Drawing.Image]::FromFile((Join-Path $root $entry.path))
        $g.DrawImage($icon,[Drawing.Rectangle]::new($x+($cellW-80)/2,$y,80,80),0,0,$icon.Width,$icon.Height,[Drawing.GraphicsUnit]::Pixel)
        $g.DrawString($entry.id,$font,$brush,[Drawing.RectangleF]::new($x+4,$y+82,$cellW-8,35))
        $icon.Dispose()
    }
    $path=Join-Path $root ('source_assets/blender/material_family/'+$name+'.png')
    $bitmap.Save($path,[Drawing.Imaging.ImageFormat]::Png);$g.Dispose();$bitmap.Dispose();Write-Output $path
}
$items=@($manifest.items | ForEach-Object { @{id=$_.id;path=('src/main/resources/assets/domesurvival/textures/item/materials_v2/'+$_.id+'.png')} })
Sheet $items 6 180 120 'items'
$blocks=@($manifest.ores | Where-Object { $_.host -ne 'deepslate_top' } | ForEach-Object { @{id=$_.texture;path=('src/main/resources/assets/domesurvival/textures/block/materials_v2/'+$_.texture+'.png')} })
$blocks+=@($manifest.storage_blocks | ForEach-Object { @{id=$_;path=('src/main/resources/assets/domesurvival/textures/block/materials_v2/'+$_.ToString()+'.png')} })
Sheet $blocks 7 144 120 'blocks'
$font.Dispose();$brush.Dispose()
