param([Parameter(Mandatory=$true)][string]$ModsDir)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Read-Entry($e) {
    $s=$e.Open()
    try {
        $r=New-Object IO.StreamReader($s,[Text.Encoding]::UTF8,$true)
        try { $r.ReadToEnd() } finally { $r.Dispose() }
    } finally { $s.Dispose() }
}

function Has-Mod([string]$id,[array]$jars) {
    foreach($j in $jars){
        try{
            $z=[IO.Compression.ZipFile]::OpenRead($j.FullName)
            try{
                $e=$z.GetEntry('META-INF/mods.toml')
                if($e){
                    $t=Read-Entry $e
                    if($t -match ('(?im)^\s*modId\s*=\s*["'']'+[regex]::Escape($id)+'["'']\s*$')){ return $true }
                }
            } finally { $z.Dispose() }
        } catch{}
    }
    return $false
}

function Patch-Jar([string]$jar,[hashtable]$rep){
    $tmp=Join-Path $env:TEMP ('DOME_COMPAT_'+[guid]::NewGuid().ToString('N'))
    $x=Join-Path $tmp 'x'
    $new=Join-Path $tmp 'new.jar'
    New-Item -ItemType Directory -Force $x|Out-Null
    $changed=0
    try{
        [IO.Compression.ZipFile]::ExtractToDirectory($jar,$x)
        Get-ChildItem $x -Recurse -File | ForEach-Object {
            if($_.Extension.ToLowerInvariant() -notin @('.json','.json5','.mcmeta','.toml','.cfg','.txt')){return}
            try{
                $t=[IO.File]::ReadAllText($_.FullName)
                $n=$t
                foreach($kv in $rep.GetEnumerator()){ $n=$n.Replace([string]$kv.Key,[string]$kv.Value) }
                if($n -cne $t){
                    [IO.File]::WriteAllText($_.FullName,$n.TrimStart([char]0xFEFF),$utf8)
                    $script:c++
                }
            }catch{}
        }
        if($script:c -gt 0){
            # ZipFile.CreateFromDirectory writes '\\' entry separators on
            # Windows.  That is legal to Windows tooling but invalid for Java
            # resource lookup and can make an embedded datapack disappear.
            # Create entries explicitly with portable '/' paths.
            $output=[IO.Compression.ZipFile]::Open($new,[IO.Compression.ZipArchiveMode]::Create)
            try {
                $archiveContentPrefix=([IO.Path]::GetFullPath((Join-Path $tmp 'x'))).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
                Get-ChildItem -LiteralPath $x -Recurse -File | ForEach-Object {
                    if(-not $_.FullName.StartsWith($archiveContentPrefix,[StringComparison]::OrdinalIgnoreCase)){
                        throw "JAR source file escaped extraction root: '$($_.FullName)'"
                    }
                    $relative=$_.FullName.Substring($archiveContentPrefix.Length).Replace('\','/')
                    if([string]::IsNullOrWhiteSpace($relative)){
                        throw "Invalid relative JAR entry path '$relative' for '$($_.FullName)'"
                    }
                    $target=$output.CreateEntry($relative,[IO.Compression.CompressionLevel]::Optimal)
                    $targetStream=$target.Open()
                    $sourceStream=[IO.File]::OpenRead($_.FullName)
                    try { $sourceStream.CopyTo($targetStream) }
                    finally { $sourceStream.Dispose(); $targetStream.Dispose() }
                }
            } finally { $output.Dispose() }
            Copy-Item $new $jar -Force
            $changed=$script:c
        }
    } finally {
        $script:c=0
        Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
    }
    return $changed
}

if(!(Test-Path $ModsDir)){ exit 0 }
$jars=@(Get-ChildItem $ModsDir -File -Filter '*.jar')
$rep=@{}
if(-not (Has-Mod 'create' $jars)){ $rep['create:framed_glass_door']='minecraft:iron_door' }
if(-not (Has-Mod 'legendarysurvivaloverhaul' $jars)){ $rep['legendarysurvivaloverhaul:medkit']='minecraft:golden_apple' }

if($rep.Count -eq 0){
    Write-Host '[RUNTIME COMPAT] No substitutions required.' -ForegroundColor Green
    exit 0
}

Write-Host '[RUNTIME COMPAT] Patching effective run\mods before ForgeGradle preparation...' -ForegroundColor Cyan
$total=0
foreach($j in $jars){
    $n=Patch-Jar $j.FullName $rep
    if($n -gt 0){
        $total += $n
        Write-Host ("[RUNTIME COMPAT] Patched {0}: {1} entries" -f $j.Name,$n) -ForegroundColor Green
    }
}
Write-Host ("[RUNTIME COMPAT] Done. Patched text entries: {0}" -f $total) -ForegroundColor Green
exit 0
