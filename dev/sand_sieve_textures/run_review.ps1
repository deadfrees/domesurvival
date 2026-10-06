$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
Set-Location -LiteralPath $projectRoot
$modsPath = [IO.Path]::GetFullPath((Join-Path $projectRoot 'run/mods'))
$holdPath = [IO.Path]::GetFullPath((Join-Path $projectRoot 'run/sand-sieve-texture-review/production_mod_hold'))
foreach ($target in @($modsPath, $holdPath)) {
    if (-not $target.StartsWith($projectRoot + '\', [StringComparison]::OrdinalIgnoreCase)) { throw "Path outside workspace: $target" }
}
New-Item -ItemType Directory -Force -Path $holdPath | Out-Null
if (@(Get-ChildItem -LiteralPath $holdPath -File).Count -ne 0) { throw 'Hold directory is not empty; inspect it before retrying.' }
$moved = [Collections.Generic.List[string]]::new()
$exitCode = 1
$checksPath = Join-Path $PSScriptRoot 'runtime/checks.txt'
if (Test-Path -LiteralPath $checksPath) {
    $archivePath = Join-Path $PSScriptRoot ('runtime/checks.before_' + [DateTime]::UtcNow.ToString('yyyyMMdd_HHmmss_fff') + '.txt')
    Move-Item -LiteralPath $checksPath -Destination $archivePath
}
try {
    # Same temporary mod hold used by RUN_DEV_FULL.bat, with restoration in finally.
    foreach ($jar in Get-ChildItem -LiteralPath $modsPath -Filter '*.jar' -File) {
        Move-Item -LiteralPath $jar.FullName -Destination (Join-Path $holdPath $jar.Name)
        $moved.Add($jar.Name)
    }
    $env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.12'
    $env:PATH = $env:JAVA_HOME + '\bin;' + $env:PATH
    $ErrorActionPreference = 'Continue'
    & .\gradlew.bat -PdomeFullDev=true -I dev/sand_sieve_textures/review.init.gradle runClient --offline --console=plain *> dev/sand_sieve_textures/client.log
    $exitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = 'Stop'
    foreach ($name in $moved) {
        $target = Join-Path $modsPath $name
        if (Test-Path -LiteralPath $target) { throw "Refusing to overwrite mod during restoration: $target" }
        Move-Item -LiteralPath (Join-Path $holdPath $name) -Destination $target
    }
    Write-Output "Restored $($moved.Count) production mod JARs."
}
exit $exitCode

