[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$projectPath = (Resolve-Path -LiteralPath $Project).Path
$gradleWrapper = Join-Path $projectPath "gradlew.bat"

if (-not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
    throw "Gradle wrapper not found: $gradleWrapper"
}

Push-Location $projectPath
try {
    & $gradleWrapper compileJava processResources --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle compileJava/processResources failed with exit code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}

Write-Host "compileJava + processResources: OK" -ForegroundColor Green

