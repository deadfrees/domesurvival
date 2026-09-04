$ErrorActionPreference = 'Stop'

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$ModsDir = Join-Path $ProjectRoot 'run\mods'
$GeneratedDir = Join-Path $PSScriptRoot 'generated'
$CacheDir = Join-Path $GeneratedDir 'fullmods'
$RuntimeGradle = Join-Path $GeneratedDir 'full_modpack_runtime.gradle'
$Snapshot = Join-Path $GeneratedDir 'full_modpack.snapshot.txt'
$FingerprintFile = Join-Path $GeneratedDir 'full_modpack.fingerprint.txt'
$MappingFile = Join-Path $GeneratedDir 'full_modpack.mapping.txt'
$GeneratorVersionFile = Join-Path $GeneratedDir 'full_modpack.generator_version.txt'
$BridgeReport = Join-Path $GeneratedDir 'mixin_srg_bridge_report.txt'
$SrgToMcp = Join-Path $ProjectRoot 'build\createSrgToMcp\output.srg'
$BridgeClassDir = Join-Path $PSScriptRoot 'tools\bin'

$GeneratorVersion = '6.9.1-alexscaves-camera-getter-guard'

New-Item -ItemType Directory -Force -Path $GeneratedDir | Out-Null
New-Item -ItemType Directory -Force -Path $CacheDir | Out-Null

if (-not (Test-Path -LiteralPath $SrgToMcp)) {
    Write-Host '[ERROR] ForgeGradle SRG->MojMap mapping is missing.' -ForegroundColor Red
    exit 1
}

if (-not (Test-Path -LiteralPath (Join-Path $BridgeClassDir 'MixinSrgBridge.class'))) {
    Write-Host '[ERROR] V6.9 MixinSrgBridge.class is missing.' -ForegroundColor Red
    exit 2
}

$jars = @(Get-ChildItem -LiteralPath $ModsDir -File -Filter '*.jar' | Sort-Object Name)

if ($jars.Count -eq 0) {
    Write-Host '[ERROR] run\mods contains no JAR files.' -ForegroundColor Red
    exit 3
}

$excludePatterns = @(
    '^curios-forge-',
    '^cofh_core-',
    '^thermal_core-',
    '^CustomNPCs',
    '^domesurvival-'
)

function Is-Excluded([string]$Name) {
    foreach ($pattern in $excludePatterns) {
        if ($Name -match $pattern) { return $true }
    }
    return $false
}

$runtimeJars = @($jars | Where-Object { -not (Is-Excluded $_.Name) })

$rows = New-Object System.Collections.Generic.List[string]
foreach ($jar in $jars) {
    $hash = (Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $rows.Add("$($jar.Name)|$($jar.Length)|$hash")
}

$payload = [string]::Join("`n", $rows)
$sha = [System.Security.Cryptography.SHA256]::Create()

try {
    $bytes = [Text.Encoding]::UTF8.GetBytes($payload)
    $fingerprint =
        ([BitConverter]::ToString($sha.ComputeHash($bytes))).
        Replace('-', '').
        ToLowerInvariant()
}
finally {
    $sha.Dispose()
}

$oldFingerprint = ''
if (Test-Path $FingerprintFile) {
    $oldFingerprint = (Get-Content $FingerprintFile -Raw).Trim()
}

$oldVersion = ''
if (Test-Path $GeneratorVersionFile) {
    $oldVersion = (Get-Content $GeneratorVersionFile -Raw).Trim()
}

$cacheReady = (
    $oldVersion -eq $GeneratorVersion -and
    $oldFingerprint -eq $fingerprint -and
    (Test-Path $RuntimeGradle) -and
    (Test-Path $BridgeReport) -and
    (@(Get-ChildItem $CacheDir -File -Filter '*.jar').Count -eq $runtimeJars.Count)
)

if ($cacheReady) {
    $bridge = [IO.File]::ReadAllText($BridgeReport)

    if ($bridge -notmatch 'JarJar-aware Mixin SRG Bridge V6\.9' -or
        $bridge -notmatch 'Raw SRG top-level non-mixin changes:\s+0' -or
        $bridge -notmatch 'ACCESSOR\s+getF_62776_\s+->\s+getLevel' -or
        $bridge -notmatch 'RAW_TOP_MIXIN\s+m_91288_\s+->\s+getCameraEntity') {
        $cacheReady = $false
    }
}

if ($cacheReady) {
    Write-Host '[OK] FULL DEV V6.9 cache is current.' -ForegroundColor Green
    exit 0
}

Write-Host '[CACHE] Rebuilding FULL DEV V6.9 from untouched production JARs...' -ForegroundColor Yellow

Get-ChildItem $CacheDir -File -Filter '*.jar' -ErrorAction SilentlyContinue |
    Remove-Item -Force

Remove-Item $BridgeReport -Force -ErrorAction SilentlyContinue

$mappingRows = New-Object System.Collections.Generic.List[string]
$dependencyLines = New-Object System.Collections.Generic.List[string]

$index = 1

foreach ($jar in $runtimeJars) {
    $safeName = ('mod{0:d4}.jar' -f $index)
    $safeBase = [IO.Path]::GetFileNameWithoutExtension($safeName)
    $target = Join-Path $CacheDir $safeName

    Copy-Item $jar.FullName $target -Force

    $mappingRows.Add("$safeName <- $($jar.Name)")
    $coordinate = 'dome.full:{0}:1' -f $safeBase
    $dependencyLines.Add("    runtimeOnly fg.deobf('$coordinate')")

    $index++
}

Write-Host '[BRIDGE] Top-level Mixins + nested JarJar SRG remap...' -ForegroundColor Cyan

& java -cp $BridgeClassDir MixinSrgBridge $SrgToMcp $CacheDir $BridgeReport

if ($LASTEXITCODE -ne 0) {
    Write-Host '[ERROR] Scoped Mixin Bridge failed.' -ForegroundColor Red
    exit 10
}

$bridge = [IO.File]::ReadAllText($BridgeReport)

if ($bridge -notmatch 'JarJar-aware Mixin SRG Bridge V6\.9') {
    Write-Host '[ERROR] Mixin bridge report is not V6.9.' -ForegroundColor Red
    exit 11
}

if ($bridge -notmatch 'Raw SRG top-level non-mixin changes:\s+0') {
    Write-Host '[ERROR] Scope safety invariant failed.' -ForegroundColor Red
    exit 12
}

if ($bridge -notmatch 'ACCESSOR\s+getF_62776_\s+->\s+getLevel') {
    Write-Host '[ERROR] Enhanced Celestials accessor repair was not applied.' -ForegroundColor Red
    exit 13
}

if ($bridge -notmatch 'RAW_NESTED\s+m_91087_\s+->\s+getInstance') {
    Write-Host '[ERROR] Nested JarJar Minecraft.m_91087_ was not remapped.' -ForegroundColor Red
    exit 14
}

if ($bridge -notmatch 'RAW_NESTED\s+m_195834_\s+->\s+') {
    Write-Host '[ERROR] Nested JarJar DetectedVersion.m_195834_ was not remapped.' -ForegroundColor Red
    exit 15
}

if ($bridge -notmatch 'm_91288_\s+->\s+getCameraEntity') {
    Write-Host '[ERROR] ForgeGradle mapping table does not map Minecraft.m_91288_ to getCameraEntity.' -ForegroundColor Red
    exit 16
}

if ($bridge -notmatch 'RAW_TOP_MIXIN\s+m_91288_\s+->\s+getCameraEntity') {
    Write-Host '[ERROR] Alexs Caves Minecraft.getCameraEntity mixin remap was not applied.' -ForegroundColor Red
    Write-Host '        Expected: RAW_TOP_MIXIN m_91288_ -> getCameraEntity' -ForegroundColor DarkYellow
    exit 17
}

$gradleText = @"
// AUTO-GENERATED BY DomeSurvival FULL DEV V6.9
// Raw SRG patching is scoped to actual Mixin classes.
// Source fingerprint: $fingerprint

repositories {
    flatDir {
        dirs project.file('dev/generated/fullmods')
    }
}

dependencies {
$([string]::Join("`r`n", $dependencyLines))
}
"@

$coordMatches = [regex]::Matches(
    $gradleText,
    "runtimeOnly\s+fg\.deobf\('dome\.full:(mod\d{4}):1'\)"
)

if ($coordMatches.Count -ne $runtimeJars.Count) {
    Write-Host '[ERROR] Dependency coordinate count mismatch.' -ForegroundColor Red
    exit 20
}

$utf8 = New-Object Text.UTF8Encoding($false)

[IO.File]::WriteAllText($RuntimeGradle, $gradleText, $utf8)
[IO.File]::WriteAllLines($MappingFile, $mappingRows, $utf8)
[IO.File]::WriteAllLines($Snapshot, $rows, $utf8)
[IO.File]::WriteAllText($FingerprintFile, $fingerprint + "`r`n", $utf8)
[IO.File]::WriteAllText($GeneratorVersionFile, $GeneratorVersion + "`r`n", $utf8)

Write-Host ''
Write-Host '[OK] FULL DEV V6.9 cache rebuilt.' -ForegroundColor Green
Write-Host "     Production JARs: $($jars.Count)"
Write-Host "     Local deobf JARs: $($runtimeJars.Count)"
Write-Host "     Bridge report: $BridgeReport"
exit 0
