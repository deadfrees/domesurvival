[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$projectPath = (Resolve-Path -LiteralPath $Project).Path
$classpathFile = Join-Path $projectPath "build\classpath\mapAuditClasspath.txt"
if (-not (Test-Path -LiteralPath $classpathFile -PathType Leaf)) {
    throw "Missing Forge classpath file: $classpathFile"
}

$javaHome = "C:\Program Files\Java\jdk-17.0.12"
$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"
$toolClasses = Join-Path $projectPath "build\map-audit-tools"
New-Item -ItemType Directory -Path $toolClasses -Force | Out-Null

$classpath = ((Get-Content -LiteralPath $classpathFile | Where-Object { $_ }) -join ";")
& $javac -encoding UTF-8 -cp $classpath -d $toolClasses `
    (Join-Path $projectPath "dev\map_audit\WorldStorageInstaller.java")
if ($LASTEXITCODE -ne 0) { throw "WorldStorageInstaller compilation failed: $LASTEXITCODE" }

$runtimeClasspath = $toolClasses + ";" + $classpath
& $java -Xmx1G -cp $runtimeClasspath WorldStorageInstaller `
    (Join-Path $projectPath "run\saves\WASTED_TEST\region") `
    (Join-Path $projectPath "dev\map_audit\output\storage_plan.csv") `
    (Join-Path $projectPath "dev\map_audit\output\storage_install_journal.csv")
if ($LASTEXITCODE -ne 0) { throw "WorldStorageInstaller failed: $LASTEXITCODE" }
