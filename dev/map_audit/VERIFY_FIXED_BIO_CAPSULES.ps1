[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$projectPath = (Resolve-Path -LiteralPath $Project).Path
$auditRoot = Join-Path $projectPath "dev\map_audit"
$classpathFile = Join-Path $projectPath "build\classpath\mapAuditClasspath.txt"
$toolClasses = Join-Path $projectPath "build\map-audit-tools"
$javac = "C:\Program Files\Java\jdk-17.0.12\bin\javac.exe"
$java = "C:\Program Files\Java\jdk-17.0.12\bin\java.exe"

if (-not (Test-Path -LiteralPath $classpathFile -PathType Leaf)) {
    throw "Missing Forge classpath file: $classpathFile"
}
New-Item -ItemType Directory -Path $toolClasses -Force | Out-Null
$classpath = ((Get-Content -LiteralPath $classpathFile | Where-Object { $_ }) -join ";")

& $javac -encoding UTF-8 -cp $classpath -d $toolClasses `
    (Join-Path $auditRoot "WorldBioCapsuleInstaller.java")
if ($LASTEXITCODE -ne 0) { throw "WorldBioCapsuleInstaller compilation failed: $LASTEXITCODE" }

$runtimeClasspath = $toolClasses + ";" + $classpath
& $java -Xmx1G -cp $runtimeClasspath WorldBioCapsuleInstaller `
    (Join-Path $projectPath "run\saves\WASTED_TEST\region") `
    (Join-Path $auditRoot "output\storage_plan.csv") `
    (Join-Path $auditRoot "bio_capsule_placement.json") `
    (Join-Path $auditRoot "output\bio_capsule_install_journal.csv") `
    --verify
if ($LASTEXITCODE -ne 0) { throw "Fixed biological-module verification failed: $LASTEXITCODE" }

Write-Host "Fixed-map biological capsule verification passed." -ForegroundColor Green
