$ErrorActionPreference = 'Stop'
$destination = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'project'))
if (!(Test-Path -LiteralPath (Join-Path $destination 'gradlew.bat'))) { throw 'Project not found beside this script.' }
if (Test-Path -LiteralPath 'C:\domesurvival') {
    Write-Host 'C:\domesurvival already exists. It has not been changed.'
    exit 1
}
New-Item -ItemType Junction -Path 'C:\domesurvival' -Target $destination | Out-Null
Write-Host "C:\domesurvival now points to $destination"
