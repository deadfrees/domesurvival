$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$jar=Join-Path $root 'build/libs/domesurvival-0.2.0.jar'
$source=Join-Path $root 'src/main/resources/assets/domesurvival/textures/item/materials_v2'
$names=@('assets/domesurvival/textures/item/materials_v2/steel_ingot.png','assets/domesurvival/textures/item/materials_v2/steel_ingot.png.mcmeta','assets/domesurvival/textures/item/materials_v2/neosteel_ingot.png','assets/domesurvival/textures/item/materials_v2/neosteel_ingot.png.mcmeta')
$press=Join-Path $root 'src/main/resources/assets/domesurvival/textures/item/press_products'
foreach($file in Get-ChildItem -LiteralPath $press -File){$names += 'assets/domesurvival/textures/item/press_products/'+$file.Name}
$archive=[IO.Compression.ZipFile]::Open($jar,[IO.Compression.ZipArchiveMode]::Update)
try {
    foreach($name in $names){
        $old=$archive.GetEntry($name);if($null -ne $old){$old.Delete()}
        $entry=$archive.CreateEntry($name,[IO.Compression.CompressionLevel]::Optimal)
        $sourceRoot=$source
        if($name -like 'assets/domesurvival/textures/item/press_products/*'){$sourceRoot=Join-Path $root 'src/main/resources/assets/domesurvival/textures/item/press_products'}
        $stream=$entry.Open();$bytes=[IO.File]::ReadAllBytes((Join-Path $sourceRoot ([IO.Path]::GetFileName($name))));$stream.Write($bytes,0,$bytes.Length);$stream.Dispose()
    }
} finally {$archive.Dispose()}
Write-Output ('Patched '+$names.Count+' resources in '+$jar)
