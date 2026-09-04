$ErrorActionPreference = 'Stop'

$Generated = Join-Path $PSScriptRoot 'generated'
$GradleFile = Join-Path $Generated 'full_modpack_runtime.gradle'
$CacheDir = Join-Path $Generated 'fullmods'
$BridgeReport = Join-Path $Generated 'mixin_srg_bridge_report.txt'
$VersionFile = Join-Path $Generated 'full_modpack.generator_version.txt'

if (-not (Test-Path $GradleFile)) { exit 1 }
if (-not (Test-Path $BridgeReport)) { exit 2 }
if (-not (Test-Path $VersionFile)) { exit 10 }

$text = [IO.File]::ReadAllText($GradleFile)
$bridge = [IO.File]::ReadAllText($BridgeReport)
$version = ([IO.File]::ReadAllText($VersionFile)).Trim()

$coords = [regex]::Matches(
    $text,
    "runtimeOnly\s+fg\.deobf\('dome\.full:(mod\d{4}):1'\)"
)

$jars = @(Get-ChildItem $CacheDir -File -Filter '*.jar')

Write-Host "Generated local coordinates: $($coords.Count)"
Write-Host "Normalized local JARs:       $($jars.Count)"
Write-Host "Cache generator version:     $version"

if ($coords.Count -ne $jars.Count -or $coords.Count -eq 0) {
    Write-Host '[ERROR] Local dependency map is inconsistent.' -ForegroundColor Red
    exit 3
}

if ($version -ne '6.9-alexscaves-camera-guard') {
    Write-Host '[ERROR] FULL DEV cache is stale. Force a V6.9 rebuild.' -ForegroundColor Red
    exit 4
}

if ($bridge -notmatch 'JarJar-aware Mixin SRG Bridge V6\.8') {
    Write-Host '[ERROR] Mixin bridge report is missing or incompatible.' -ForegroundColor Red
    exit 5
}

if ($bridge -notmatch 'Raw SRG top-level non-mixin changes:\s+0') {
    Write-Host '[ERROR] Non-mixin raw SRG safety check failed.' -ForegroundColor Red
    exit 6
}

if ($bridge -notmatch 'f_131257_\s+->\s+value') {
    Write-Host '[ERROR] Enhanced Celestials TextColor mapping missing.' -ForegroundColor Red
    exit 7
}

if ($bridge -notmatch 'ACCESSOR\s+getF_62776_\s+->\s+getLevel') {
    Write-Host '[ERROR] Enhanced Celestials LevelChunk accessor mapping missing.' -ForegroundColor Red
    exit 8
}

if ($bridge -notmatch 'RAW_NESTED\s+m_91087_\s+->\s+getInstance') {
    Write-Host '[ERROR] Nested TRansition Minecraft.getInstance remap missing.' -ForegroundColor Red
    exit 9
}

if ($bridge -notmatch 'RAW_NESTED\s+m_195834_\s+->\s+') {
    Write-Host '[ERROR] Nested TRansition DetectedVersion remap missing.' -ForegroundColor Red
    exit 11
}

if ($bridge -notmatch 'RAW_TOP_MIXIN\s+m_91288_\s+->\s+setCameraEntity') {
    Write-Host '[ERROR] Alexs Caves Minecraft.setCameraEntity mixin remap missing.' -ForegroundColor Red
    Write-Host '        Expected: RAW_TOP_MIXIN m_91288_ -> setCameraEntity' -ForegroundColor DarkYellow
    exit 12
}

Write-Host '[OK] V6.9 FULL DEV cache validation passed.' -ForegroundColor Green
exit 0
