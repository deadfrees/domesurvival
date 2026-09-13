param([switch]$AllTiers)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$taskJar = Join-Path $taskRoot 'build/libs/domesurvival-0.2.0.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskArchive = [IO.Compression.ZipFile]::OpenRead($taskJar)
$taskChecks = @()
try {
    $taskStatic = Get-Content (Join-Path $PSScriptRoot 'tier1_static_checks.json') -Raw | ConvertFrom-Json
    $taskResources = @($taskStatic.changed) + @($taskStatic.added)
    if ($AllTiers) {
        $taskFamilyStatic = Get-Content (Join-Path $PSScriptRoot 'tiers23_static_checks.json') -Raw | ConvertFrom-Json
        $taskResources += @($taskFamilyStatic.changed) + @($taskFamilyStatic.added)
    }
    foreach ($taskRelative in $taskResources) {
        $taskEntryName = $taskRelative.Replace('src/main/resources/', '')
        $taskEntry = $taskArchive.GetEntry($taskEntryName)
        if ($null -eq $taskEntry) { throw "Missing JAR entry: $taskEntryName" }
        $taskStream = $taskEntry.Open()
        $taskHasher = [Security.Cryptography.SHA256]::Create()
        try { $taskDigest = [BitConverter]::ToString($taskHasher.ComputeHash($taskStream)).Replace('-', '') }
        finally { $taskStream.Dispose(); $taskHasher.Dispose() }
        $taskSourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $taskRoot $taskRelative)).Hash
        if ($taskDigest -ne $taskSourceHash) { throw "Stale JAR entry: $taskEntryName" }
        $taskChecks += $taskEntryName
    }
    if (@($taskArchive.Entries | Where-Object { $_.FullName -match 'energyprobe|EnergyPipeVisualProbe|EnergyPipeFamilyProbe' }).Count -ne 0) {
        throw 'Test probe unexpectedly packaged in production JAR'
    }
} finally { $taskArchive.Dispose() }
$taskResult = [ordered]@{
    status = 'PASS'
    jar = 'build/libs/domesurvival-0.2.0.jar'
    sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $taskJar).Hash.ToLowerInvariant()
    pipe_resources_match_source = $taskChecks
    test_probe_packaged = $false
}
$taskJson = ($taskResult | ConvertTo-Json -Depth 8) + [Environment]::NewLine
$taskReportName = if ($AllTiers) { 'family_jar_checks.json' } else { 'tier1_jar_checks.json' }
[IO.File]::WriteAllText((Join-Path $PSScriptRoot $taskReportName), $taskJson, (New-Object Text.UTF8Encoding($false)))
$taskJson
