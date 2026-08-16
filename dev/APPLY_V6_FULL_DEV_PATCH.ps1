$ErrorActionPreference = 'Stop'

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$BuildGradle = Join-Path $ProjectRoot 'build.gradle'
$BackupDir = Join-Path $PSScriptRoot 'backups'
$BackupFile = Join-Path $BackupDir 'build.gradle.before_v6_0_full_dev.bak'

if (-not (Test-Path -LiteralPath $BuildGradle)) {
    Write-Host "[ERROR] build.gradle not found: $BuildGradle" -ForegroundColor Red
    exit 1
}

New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
Copy-Item -LiteralPath $BuildGradle -Destination $BackupFile -Force

$text = [System.IO.File]::ReadAllText($BuildGradle)

if ($text -match 'DEV_V6_0_FULL_MODPACK_RUNTIME') {
    Write-Host '[OK] build.gradle already contains the V6.0 FULL DEV hook.' -ForegroundColor Green
    exit 0
}

$hook = @'

// DEV_V6_0_FULL_MODPACK_RUNTIME
// Full modpack development mode.
// The generated script contains ONLY runtimeOnly fg.deobf(...) local mod dependencies.
// It is activated explicitly with -PdomeFullDev=true.
if (project.hasProperty('domeFullDev')) {
    def domeFullDevScript = file('dev/generated/full_modpack_runtime.gradle')
    if (!domeFullDevScript.exists()) {
        throw new GradleException(
            'FULL DEV runtime is not prepared. Run dev\\RUN_DEV_FULL.bat first.'
        )
    }
    apply from: domeFullDevScript
}
'@

if (-not $text.EndsWith("`n")) {
    $text += "`r`n"
}

$text += "`r`n" + $hook + "`r`n"

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($BuildGradle, $text, $utf8NoBom)

Write-Host '[OK] V6.0 FULL DEV hook added to build.gradle.' -ForegroundColor Green
Write-Host "Backup: $BackupFile"
exit 0
