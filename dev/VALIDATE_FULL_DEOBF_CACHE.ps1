$ErrorActionPreference = 'Stop'

$Generated = Join-Path $PSScriptRoot 'generated'
$GradleFile = Join-Path $Generated 'full_modpack_runtime.gradle'
$CacheDir = Join-Path $Generated 'fullmods'
$BridgeReport = Join-Path $Generated 'mixin_srg_bridge_report.txt'

if (-not (Test-Path $GradleFile)) { exit 1 }
if (-not (Test-Path $BridgeReport)) { exit 2 }

$text = [IO.File]::ReadAllText($GradleFile)
$bridge = [IO.File]::ReadAllText($BridgeReport)

$coords = [regex]::Matches(
    $text,
    "runtimeOnly\s+fg\.deobf\('dome\.full:(mod\d{4}):1'\)"
)

$jars = @(Get-ChildItem $CacheDir -File -Filter '*.jar')

Write-Host "Generated local coordinates: $($coords.Count)"
Write-Host "Normalized local JARs:       $($jars.Count)"

if ($coords.Count -ne $jars.Count -or $coords.Count -eq 0) {
    Write-Host '[ERROR] Local dependency map is inconsistent.' -ForegroundColor Red
    exit 3
}

if ($bridge -notmatch 'JarJar-aware Mixin SRG Bridge V6\.8') {
    Write-Host '[ERROR] Bridge report is not V6.7.' -ForegroundColor Red
    exit 4
}

if ($bridge -notmatch 'Raw SRG top-level non-mixin changes:\s+0') {
    Write-Host '[ERROR] Non-mixin raw SRG safety check failed.' -ForegroundColor Red
    exit 5
}

if ($bridge -notmatch 'f_131257_\s+->\s+value') {
    Write-Host '[ERROR] Enhanced Celestials TextColor mapping missing.' -ForegroundColor Red
    exit 6
}

if ($bridge -notmatch 'ACCESSOR\s+getF_62776_\s+->\s+getLevel') {
    Write-Host '[ERROR] Enhanced Celestials LevelChunk accessor mapping missing.' -ForegroundColor Red
    exit 7
}


if ($bridge -notmatch 'RAW_NESTED\s+m_91087_\s+->\s+getInstance') {
    Write-Host '[ERROR] Nested TRansition Minecraft.getInstance remap missing.' -ForegroundColor Red
    exit 8
}

if ($bridge -notmatch 'RAW_NESTED\s+m_195834_\s+->\s+') {
    Write-Host '[ERROR] Nested TRansition DetectedVersion remap missing.' -ForegroundColor Red
    exit 9
}

Write-Host '[OK] V6.8 JarJar-aware bridge validation passed.' -ForegroundColor Green
exit 0
