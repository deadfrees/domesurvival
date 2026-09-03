$ErrorActionPreference = 'Stop'

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$Target = Join-Path $ProjectRoot 'run\mods'
$Quarantine = Join-Path $ProjectRoot 'run\mods_sync_quarantine'

New-Item -ItemType Directory -Force -Path $Target | Out-Null
New-Item -ItemType Directory -Force -Path $Quarantine | Out-Null

Write-Host '============================================================'
Write-Host 'Dome Survival - EFFECTIVE MODPACK SYNC V6.8'
Write-Host 'External canonical source OR local physical baseline'
Write-Host '============================================================'
Write-Host ''

# Recover only temporary ACTIVE dev holds.
# run\mods_disabled is intentionally never bulk-restored.
foreach ($rel in @('run\mods_dev_hold', 'run\mods_dev_hold_all')) {
    $dir = Join-Path $ProjectRoot $rel

    if (-not (Test-Path -LiteralPath $dir)) {
        continue
    }

    foreach ($jar in Get-ChildItem -LiteralPath $dir -File -Filter '*.jar' -ErrorAction SilentlyContinue) {
        $dest = Join-Path $Target $jar.Name

        if (-not (Test-Path -LiteralPath $dest)) {
            Write-Host "[RECOVER DEV HOLD] $($jar.Name)"
            Move-Item -LiteralPath $jar.FullName -Destination $dest -Force
        }
    }
}

# Prefer a real external approved source when available.
$canonical = $null
$selfCanonical = $false

if ($env:DOMESURVIVAL_MODPACK_SOURCE -and
    (Test-Path -LiteralPath $env:DOMESURVIVAL_MODPACK_SOURCE)) {

    $canonical = (Resolve-Path -LiteralPath $env:DOMESURVIVAL_MODPACK_SOURCE).Path
    Write-Host "[SOURCE] Explicit approved source: $canonical" -ForegroundColor Cyan

}
elseif (Test-Path -LiteralPath 'C:\Minecraft\DomeSurvival_PROD_TEST\mods') {

    $canonical = (Resolve-Path -LiteralPath 'C:\Minecraft\DomeSurvival_PROD_TEST\mods').Path
    Write-Host "[SOURCE] Production approved source: $canonical" -ForegroundColor Cyan

}
elseif (Test-Path -LiteralPath (Join-Path $ProjectRoot 'modpack\mods')) {

    $canonical = (Resolve-Path -LiteralPath (Join-Path $ProjectRoot 'modpack\mods')).Path
    Write-Host "[SOURCE] Project-local approved source: $canonical" -ForegroundColor Cyan

}
else {
    $existing = @(Get-ChildItem -LiteralPath $Target -File -Filter '*.jar' -ErrorAction SilentlyContinue)

    if ($existing.Count -lt 20) {
        Write-Host '[ERROR] No approved external source exists and run\mods is not a complete physical pack.' -ForegroundColor Red
        Write-Host ''
        Write-Host 'Install the approved modpack once into:'
        Write-Host "  $Target"
        Write-Host ''
        Write-Host 'or set an optional repair source:'
        Write-Host '  setx DOMESURVIVAL_MODPACK_SOURCE "D:\path\to\approved\mods"'
        exit 1
    }

    # Developer 2 may legitimately have only the physical runtime pack.
    # This is not a symlink/reference: run\mods itself becomes the local baseline.
    $canonical = $Target
    $selfCanonical = $true

    Write-Host '[SOURCE] No external source found.' -ForegroundColor Yellow
    Write-Host "[SOURCE] Using installed physical run\mods as local baseline ($($existing.Count) JARs)." -ForegroundColor Cyan
}

$canonicalJars = @(Get-ChildItem -LiteralPath $canonical -File -Filter '*.jar' | Sort-Object Name)

if ($canonicalJars.Count -lt 20) {
    Write-Host "[ERROR] Selected baseline contains only $($canonicalJars.Count) JARs." -ForegroundColor Red
    exit 2
}

# Project-pinned hard dependencies. They may be absent from an external
# production baseline, so they are overlaid onto the effective pack.
$overlays = @(
    [pscustomobject]@{
        Name = 'Curios'
        FileName = 'curios-forge-5.14.1+1.20.1.jar'
        Url = 'https://maven.theillusivec4.top/top/theillusivec4/curios/curios-forge/5.14.1%2B1.20.1/curios-forge-5.14.1%2B1.20.1.jar'
    },
    [pscustomobject]@{
        Name = 'Thermal Core'
        FileName = 'thermal_core-1.20.1-11.0.6.24.jar'
        Url = 'https://maven.covers1624.net/repository/maven-hosted/com/teamcofh/thermal_core/1.20.1-11.0.6.24/thermal_core-1.20.1-11.0.6.24.jar'
    },
    [pscustomobject]@{
        Name = 'CoFH Core'
        FileName = 'cofh_core-1.20.1-11.0.2.56.jar'
        Url = 'https://maven.covers1624.net/repository/maven-hosted/com/teamcofh/cofh_core/1.20.1-11.0.2.56/cofh_core-1.20.1-11.0.2.56.jar'
    },
    [pscustomobject]@{
        Name = 'McJtyLib'
        FileName = 'mcjtylib-1.20-8.0.8.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/233105/files/8256165/download'
    },
    [pscustomobject]@{
        Name = 'The Lost Cities'
        FileName = 'lostcities-1.20-7.5.2.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/269024/files/8644739/download'
    },
    [pscustomobject]@{
        Name = 'When Dungeons Arise'
        FileName = 'DungeonsArise-1.20.1-2.1.57-release.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/442508/files/4798432/download'
    },
    [pscustomobject]@{
        Name = 'Loot Integrations'
        FileName = 'lootintegrations-1.20.1-4.7.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/580689/files/6640968/download'
    },
    [pscustomobject]@{
        Name = "YUNG's Better Dungeons"
        FileName = 'YungsBetterDungeons-1.20-Forge-4.0.4.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/510089/files/5271360/download'
    },
    [pscustomobject]@{
        Name = "YUNG's Better Strongholds"
        FileName = 'YungsBetterStrongholds-1.20-Forge-4.0.3.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/465575/files/4769083/download'
    },
    [pscustomobject]@{
        Name = 'Lost City R.E.A.P Tweaks (Yulari)'
        FileName = 'yulari-1.20.1-lostcities.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/1559238/files/8437678/download'
    },
    [pscustomobject]@{
        Name = "YUNG's Better Desert Temples"
        FileName = 'YungsBetterDesertTemples-1.20-Forge-3.0.3.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/631016/files/4769439/download'
    },
    [pscustomobject]@{
        Name = 'Dungeon Crawl'
        FileName = 'Dungeon Crawl-1.20.1-2.3.15.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/324973/files/6047153/download'
    },
    [pscustomobject]@{
        Name = 'Better Archeology'
        FileName = 'betterarcheology-1.2.1-1.20.1.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/835687/files/5693368/download'
    },
    [pscustomobject]@{
        Name = "SuperMartijn642's Config Lib (Better Archeology dependency)"
        FileName = 'supermartijn642configlib-1.1.8-forge-mc1.20.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/438332/files/4715408/download'
    },
    [pscustomobject]@{
        Name = 'The Graveyard'
        FileName = 'The_Graveyard_3.1_(FORGE)_for_1.20.1.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/531188/files/5114579/download'
    },
    [pscustomobject]@{
        Name = 'GeckoLib (The Graveyard dependency)'
        FileName = 'geckolib-forge-1.20.1-4.4.9.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/388172/files/5675221/download'
    },
    [pscustomobject]@{
        Name = "Alex's Caves"
        FileName = 'alexscaves-2.0.2.jar'
        Url = 'https://www.curseforge.com/api/v1/mods/924854/files/5848216/download'
    }
)

function Test-ModJar([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        return $false
    }

    $file = Get-Item -LiteralPath $Path

    # Small server-side utility mods (for example Loot Integrations) can be
    # well below 50 KiB while still being valid Forge JARs.  The ZIP and
    # META-INF/mods.toml checks below are the authoritative validation.
    if ($file.Length -lt 10000) {
        return $false
    }

    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue

        $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)

        try {
            return $null -ne (
                $zip.Entries |
                Where-Object { $_.FullName -ieq 'META-INF/mods.toml' } |
                Select-Object -First 1
            )
        }
        finally {
            $zip.Dispose()
        }
    }
    catch {
        return $false
    }
}

# With an external canonical source, reconcile the active filename set.
# With self-canonical run\mods, do not quarantine its own installed files.
if (-not $selfCanonical) {
    $expected = @{}

    foreach ($jar in $canonicalJars) {
        $expected[$jar.Name.ToLowerInvariant()] = $true
    }

    foreach ($overlay in $overlays) {
        $expected[$overlay.FileName.ToLowerInvariant()] = $true
    }

    foreach ($activeJar in @(Get-ChildItem -LiteralPath $Target -File -Filter '*.jar' | Sort-Object Name)) {
        if (-not $expected.ContainsKey($activeJar.Name.ToLowerInvariant())) {
            $dest = Join-Path $Quarantine $activeJar.Name

            if (Test-Path -LiteralPath $dest) {
                $stamp = Get-Date -Format 'yyyyMMdd_HHmmssfff'
                $base = [IO.Path]::GetFileNameWithoutExtension($activeJar.Name)
                $dest = Join-Path $Quarantine "$base.$stamp.jar"
            }

            Write-Host "[QUARANTINE EXTRA] $($activeJar.Name)" -ForegroundColor Yellow
            Move-Item -LiteralPath $activeJar.FullName -Destination $dest
        }
    }

    foreach ($sourceJar in $canonicalJars) {
        $dest = Join-Path $Target $sourceJar.Name

        if (-not (Test-Path -LiteralPath $dest)) {
            Write-Host "[COPY CANONICAL] $($sourceJar.Name)"
            Copy-Item -LiteralPath $sourceJar.FullName -Destination $dest -Force
            continue
        }

        # Exact byte repair if the same filename differs.
        $srcHash = (Get-FileHash -LiteralPath $sourceJar.FullName -Algorithm SHA256).Hash
        $dstHash = (Get-FileHash -LiteralPath $dest -Algorithm SHA256).Hash

        if ($srcHash -ne $dstHash) {
            $stamp = Get-Date -Format 'yyyyMMdd_HHmmssfff'
            $base = [IO.Path]::GetFileNameWithoutExtension($sourceJar.Name)
            $backup = Join-Path $Quarantine "$base.replaced.$stamp.jar"

            Write-Host "[REPAIR CANONICAL] $($sourceJar.Name)" -ForegroundColor Yellow
            Move-Item -LiteralPath $dest -Destination $backup -Force
            Copy-Item -LiteralPath $sourceJar.FullName -Destination $dest -Force
        }
    }
}

# Restore pinned overlays from safe local locations first; download only if absent.
$localOverlaySources = @(
    $Quarantine,
    (Join-Path $ProjectRoot 'run\mods_disabled'),
    (Join-Path $ProjectRoot 'modpack\mods'),
    'C:\Minecraft\DomeSurvival_PROD_TEST\mods'
) | Where-Object { Test-Path -LiteralPath $_ }

foreach ($overlay in $overlays) {
    $dest = Join-Path $Target $overlay.FileName

    if (Test-ModJar $dest) {
        Write-Host "[OK OVERLAY] $($overlay.Name): $($overlay.FileName)" -ForegroundColor Green
        continue
    }

    $found = $null

    foreach ($sourceDir in $localOverlaySources) {
        $candidate = Join-Path $sourceDir $overlay.FileName

        if (Test-ModJar $candidate) {
            $found = $candidate
            break
        }
    }

    if ($found) {
        Write-Host "[RESTORE OVERLAY] $($overlay.Name) <- $found" -ForegroundColor Cyan
        Copy-Item -LiteralPath $found -Destination $dest -Force
    }
    else {
        Write-Host "[DOWNLOAD OVERLAY] $($overlay.Name)" -ForegroundColor Cyan

        try {
            Invoke-WebRequest -UseBasicParsing -Uri $overlay.Url -OutFile $dest
        }
        catch {
            Write-Host "[ERROR] Unable to obtain $($overlay.Name)." -ForegroundColor Red
            Write-Host $_.Exception.Message
            exit 10
        }

        if (-not (Test-ModJar $dest)) {
            Remove-Item -LiteralPath $dest -Force -ErrorAction SilentlyContinue
            Write-Host "[ERROR] Downloaded $($overlay.Name) is not a valid Forge mod JAR." -ForegroundColor Red
            exit 11
        }
    }

    Write-Host "[OK OVERLAY] $($overlay.Name): $($overlay.FileName)" -ForegroundColor Green
}

# WORLDGEN_COMPAT_V2
# Run after canonical reconciliation, before PREPARE_FULL_DEV_RUNTIME.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'PATCH_FULL_MODPACK_RUNTIME_COMPAT.ps1') -ModsDir $Target
if ($LASTEXITCODE -ne 0) {
    Write-Host '[ERROR] Runtime compatibility stage failed.' -ForegroundColor Red
    exit 30
}
$active = @(Get-ChildItem -LiteralPath $Target -File -Filter '*.jar' | Sort-Object Name)

# Project integration sanity checks.
$required = [ordered]@{
    'Curios'       = '^curios-forge-'
    'CoFH Core'    = '^cofh_core-'
    'Thermal Core' = '^thermal_core-'
    'CustomNPCs'   = 'custom.*npcs.*\.jar$'
    'Ad Astra'     = '^ad_astra-'
    'Architectury' = '^architectury-'
}

$missing = $false

foreach ($entry in $required.GetEnumerator()) {
    $match = $active |
        Where-Object { $_.Name -match $entry.Value } |
        Select-Object -First 1

    if ($null -eq $match) {
        Write-Host "[MISSING REQUIRED] $($entry.Key)" -ForegroundColor Red
        $missing = $true
    }
    else {
        Write-Host "[OK] $($entry.Key): $($match.Name)" -ForegroundColor Green
    }
}

if ($missing) {
    Write-Host ''
    Write-Host '[ERROR] Installed physical pack is incomplete.' -ForegroundColor Red

    if ($selfCanonical) {
        Write-Host 'Re-copy the approved full modpack once into run\mods, then rerun setup.'
    }

    exit 20
}

# Store only a local generated snapshot; this remains ignored by Git.
$Generated = Join-Path $PSScriptRoot 'generated'
$Snapshot = Join-Path $Generated 'effective_active_modpack.txt'

New-Item -ItemType Directory -Force -Path $Generated | Out-Null

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("SOURCE=$canonical")
$lines.Add("SELF_CANONICAL=$selfCanonical")
$lines.Add("COUNT=$($active.Count)")

foreach ($jar in $active) {
    $hash = (Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    $lines.Add("$($jar.Name)|$($jar.Length)|$hash")
}

[IO.File]::WriteAllLines(
    $Snapshot,
    $lines,
    (New-Object Text.UTF8Encoding($false))
)

Write-Host ''
Write-Host "[OK] EFFECTIVE FULL MODPACK READY: $($active.Count) physical JARs." -ForegroundColor Green

if ($selfCanonical) {
    Write-Host '[OK] Developer-local physical baseline mode is active.' -ForegroundColor Green
}

Write-Host 'run\mods_disabled was not bulk-restored.'
exit 0
