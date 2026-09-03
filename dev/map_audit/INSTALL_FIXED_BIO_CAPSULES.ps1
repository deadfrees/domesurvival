[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$projectPath = (Resolve-Path -LiteralPath $Project).Path
$auditRoot = Join-Path $projectPath "dev\map_audit"
$outputRoot = Join-Path $auditRoot "output"
$regionRoot = Join-Path $projectPath "run\saves\WASTED_TEST\region"
$storagePlanPath = Join-Path $outputRoot "storage_plan.csv"
$capsulePlanPath = Join-Path $auditRoot "bio_capsule_placement.json"
$candidateSitesPath = Join-Path $auditRoot "candidate_sites.json"
$journalPath = Join-Path $outputRoot "bio_capsule_install_journal.csv"
$classpathFile = Join-Path $projectPath "build\classpath\mapAuditClasspath.txt"

foreach ($required in @($regionRoot, $storagePlanPath, $capsulePlanPath, $candidateSitesPath, $classpathFile)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Required path is missing: $required"
    }
}

$jcmd = "C:\Program Files\Java\jdk-17.0.12\bin\jcmd.exe"
$activeJava = if (Test-Path -LiteralPath $jcmd) { (& $jcmd -l 2>$null) -join "`n" } else { "" }
if ($activeJava -match "BootstrapLauncher|GradleWrapperMain.*run(Client|Server)") {
    throw "Minecraft/Forge is running. Close the client or server before editing region files."
}

$storageRows = @(Import-Csv -LiteralPath $storagePlanPath)
$storageBySite = @{}
foreach ($row in $storageRows) {
    if ($storageBySite.ContainsKey($row.site_id)) {
        throw "Duplicate site in storage plan: $($row.site_id)"
    }
    $storageBySite[$row.site_id] = $row
}

$capsulePlan = Get-Content -LiteralPath $capsulePlanPath -Raw -Encoding utf8 | ConvertFrom-Json
$targets = New-Object System.Collections.Generic.List[object]
foreach ($entry in $capsulePlan.guaranteed) {
    foreach ($slot in @($entry.first, $entry.second)) {
        if (-not $storageBySite.ContainsKey($slot.site)) {
            throw "Capsule site has no storage target: $($slot.site)"
        }
        $targets.Add($storageBySite[$slot.site])
    }
}
foreach ($site in $capsulePlan.optional_random_sites) {
    if (-not $storageBySite.ContainsKey($site)) {
        throw "Optional capsule site has no storage target: $site"
    }
    $targets.Add($storageBySite[$site])
}
foreach ($row in @($storageRows | Where-Object status -eq "ADD")) {
    $targets.Add($row)
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupRoot = Join-Path $projectPath ("_manual_backups\bio_capsule_loot_" + $timestamp)
$backupRegionRoot = Join-Path $backupRoot "region"
New-Item -ItemType Directory -Path $backupRegionRoot -Force | Out-Null

$regionNames = @($targets | ForEach-Object {
    $regionX = [int][Math]::Floor(([int]$_.x) / 512.0)
    $regionZ = [int][Math]::Floor(([int]$_.z) / 512.0)
    "r.$regionX.$regionZ.mca"
} | Sort-Object -Unique)
foreach ($regionName in $regionNames) {
    $source = Join-Path $regionRoot $regionName
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
        throw "Target region is missing: $source"
    }
    Copy-Item -LiteralPath $source -Destination (Join-Path $backupRegionRoot $regionName) -Force
}
$backupRoot | Set-Content -LiteralPath (Join-Path $outputRoot "bio_capsule_backup_path.txt") -Encoding utf8

$javaHome = "C:\Program Files\Java\jdk-17.0.12"
$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"
$toolClasses = Join-Path $projectPath "build\map-audit-tools"
New-Item -ItemType Directory -Path $toolClasses -Force | Out-Null
$classpath = ((Get-Content -LiteralPath $classpathFile | Where-Object { $_ }) -join ";")

& $javac -encoding UTF-8 -cp $classpath -d $toolClasses `
    (Join-Path $auditRoot "WorldStorageInstaller.java") `
    (Join-Path $auditRoot "WorldBioCapsuleInstaller.java")
if ($LASTEXITCODE -ne 0) { throw "Map installer compilation failed: $LASTEXITCODE" }

$runtimeClasspath = $toolClasses + ";" + $classpath
$addRows = @($storageRows | Where-Object status -eq "ADD")
if ($addRows.Count -gt 0) {
    & $java -Xmx1G -cp $runtimeClasspath WorldStorageInstaller `
        $regionRoot $storagePlanPath (Join-Path $outputRoot "storage_install_journal.csv")
    if ($LASTEXITCODE -ne 0) { throw "Missing-storage installation failed: $LASTEXITCODE" }

    & $javac -encoding UTF-8 -d $toolClasses (Join-Path $auditRoot "WorldMapAudit.java")
    if ($LASTEXITCODE -ne 0) { throw "WorldMapAudit compilation failed: $LASTEXITCODE" }
    & $java -Xmx2G -cp $toolClasses WorldMapAudit `
        $regionRoot $outputRoot -5 5 -5 5 2 $candidateSitesPath
    if ($LASTEXITCODE -ne 0) { throw "Post-storage map audit failed: $LASTEXITCODE" }
}

& $java -Xmx1G -cp $runtimeClasspath WorldBioCapsuleInstaller `
    $regionRoot $storagePlanPath $capsulePlanPath $journalPath
if ($LASTEXITCODE -ne 0) { throw "Biological-module installation failed: $LASTEXITCODE" }

& $java -Xmx1G -cp $runtimeClasspath WorldBioCapsuleInstaller `
    $regionRoot $storagePlanPath $capsulePlanPath $journalPath --verify
if ($LASTEXITCODE -ne 0) { throw "Biological-module verification failed: $LASTEXITCODE" }

Write-Host "Fixed-map biological capsules installed and verified." -ForegroundColor Green
Write-Host "Backed up regions: $($regionNames.Count)"
Write-Host "Backup: $backupRoot"
Write-Host "Journal: $journalPath"
