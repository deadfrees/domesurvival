$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$assets=Join-Path $root 'src/main/resources/assets/domesurvival'
$manifest=Get-Content (Join-Path $PSScriptRoot 'manifest.json') -Raw | ConvertFrom-Json
$entries=@($manifest.items)+@($manifest.ores)
$entries+=@($manifest.storage_blocks | ForEach-Object { [pscustomobject]@{id=$_;kind='storage';source=('src/main/resources/assets/domesurvival/textures/block/'+$_+'.png')} })
$exact=0; $powders=0
foreach($entry in $entries){
    $category=if($entry.kind -in @('ore','storage')){'block'}else{'item'}
    $compareAlpha=$true
    if($entry.id -eq 'neosteel_ingot'){$compareAlpha=$false;$source=$null}
    else {
        $sourcePath=Join-Path $root $entry.source
        if($entry.id -eq 'steel_ingot'){$sourcePath=Join-Path $assets 'textures/item/voltarium_ingot.png'}
        $source=[Drawing.Bitmap]::new($sourcePath)
    }
    $dest=[Drawing.Bitmap]::new((Join-Path $assets ('textures/'+$category+'/materials_v2/'+$entry.id+'.png')))
    if($dest.Width -ne 32 -or $dest.Height -ne 32){throw ('Wrong dimensions: '+$entry.id)}
    for($y=0;$y -lt 32;$y++){for($x=0;$x -lt 32;$x++){
        if($compareAlpha){$a=$source.GetPixel([int][Math]::Floor($x*$source.Width/32),[int][Math]::Floor($y*$source.Height/32))}
        $b=$dest.GetPixel($x,$y)
        if($compareAlpha -and $a.A -ne $b.A){throw ('Changed silhouette: '+$entry.id)}
        if($compareAlpha -and $entry.kind -ne 'powder' -and $entry.id -ne 'steel_ingot' -and $a.A -gt 0 -and $a.ToArgb() -ne $b.ToArgb()){throw ('Changed original pixel: '+$entry.id+' '+$x+','+$y)}
    }}
    if($null -ne $source){$source.Dispose()};$dest.Dispose()
    if($entry.kind -eq 'powder'){$powders++}elseif($entry.id -notin @('steel_ingot','neosteel_ingot')){$exact++}
}
$solarite=0
$baseline=Join-Path $PSScriptRoot 'baseline'
Get-ChildItem -LiteralPath (Join-Path $baseline 'src/main/resources/assets/domesurvival/models') -Recurse -File -Filter '*solarite*.json' | ForEach-Object {
    $relative=$_.FullName.Substring($baseline.Length+1)
    if((Get-FileHash -LiteralPath $_.FullName).Hash -ne (Get-FileHash -LiteralPath (Join-Path $root $relative)).Hash){throw ('Solarite changed: '+$relative)}
    $solarite++
}
$result=@{result='PASS';exact_original_designs=$exact;gunpowder_silhouettes=$powders;unchanged_solarite_models=$solarite}
$result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'pixel_validation.json') -Encoding UTF8
$result | ConvertTo-Json
