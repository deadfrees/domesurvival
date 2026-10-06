$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
Set-Location -LiteralPath $projectRoot
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.12'
$env:PATH = $env:JAVA_HOME + '\bin;' + $env:PATH
$ErrorActionPreference = 'Continue'
& .\gradlew.bat test build --offline --console=plain *> dev/machine_gauges/build.log
exit $LASTEXITCODE

