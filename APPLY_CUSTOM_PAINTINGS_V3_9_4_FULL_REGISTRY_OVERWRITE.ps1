$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Payload = Join-Path $PSScriptRoot "PATCH_PAYLOAD"
$VariantJava = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\registry\ModPaintingVariants.java"
$PayloadJava = Join-Path $Payload "src\main\java\com\wasted\domesurvival\forge\registry\ModPaintingVariants.java"
$TextureDir = Join-Path $Root "src\main\resources\assets\domesurvival\textures\painting"
$Gradle = Join-Path $Root "gradlew.bat"

if (-not (Test-Path -LiteralPath $VariantJava)) {
    throw "ModPaintingVariants.java not found. Install the working Custom Paintings V3.8 first."
}
if (-not (Test-Path -LiteralPath $PayloadJava)) {
    throw "V3.9.4 payload is incomplete: ModPaintingVariants.java missing."
}
if (-not (Test-Path -LiteralPath $TextureDir)) {
    throw "Painting texture directory not found: $TextureDir"
}
if (-not (Test-Path -LiteralPath $Gradle)) {
    throw "gradlew.bat not found: $Gradle"
}

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\custom_paintings_v3_9_4_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null

Copy-Item -LiteralPath $VariantJava -Destination (Join-Path $Backup "ModPaintingVariants.java") -Force

# Backup the texture files that this hotfix carries.
$PayloadTextureRoot = Join-Path $Payload "src\main\resources\assets\domesurvival\textures\painting"
if (Test-Path -LiteralPath $PayloadTextureRoot) {
    Get-ChildItem -LiteralPath $PayloadTextureRoot -File | ForEach-Object {
        $Current = Join-Path $TextureDir $_.Name
        if (Test-Path -LiteralPath $Current) {
            Copy-Item -LiteralPath $Current -Destination (Join-Path $Backup $_.Name) -Force
        }
    }
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "DomeSurvival CUSTOM PAINTINGS V3.9.4 - FULL REGISTRY OVERWRITE" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "[INFO] Previous V3.9.x scripts are no longer used to modify Java." -ForegroundColor Yellow

# KEY FIX:
# Do not parse or patch the current Java text at all.
# Replace the full registry file with the known-good V3.9.4 source.
Copy-Item -LiteralPath $PayloadJava -Destination $VariantJava -Force

# Copy only resource payload files.
$PayloadResources = Join-Path $Payload "src\main\resources"
if (Test-Path -LiteralPath $PayloadResources) {
    Get-ChildItem -LiteralPath $PayloadResources -Recurse -File | ForEach-Object {
        $relative = $_.FullName.Substring($PayloadResources.Length).TrimStart('\','/')
        $destination = Join-Path (Join-Path $Root "src\main\resources") $relative
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
    }
}

# Byte-for-byte verification: project registry must now equal payload registry.
$ExpectedHash = (Get-FileHash -LiteralPath $PayloadJava -Algorithm SHA256).Hash
$ActualHash = (Get-FileHash -LiteralPath $VariantJava -Algorithm SHA256).Hash

if ($ExpectedHash -ne $ActualHash) {
    throw "V3.9.4 registry copy verification failed. expected=$ExpectedHash actual=$ActualHash"
}

$Registry = [IO.File]::ReadAllText($VariantJava, [Text.Encoding]::UTF8)

$RequiredFragments = @(
    'register("06_relaxing_on_grass", () -> new PaintingVariant(48, 64))',
    'register("compact_03_airsoft_team", () -> new PaintingVariant(48, 32))',
    'register("07_pink_hat_portrait", () -> new PaintingVariant(16, 16))',
    'register("01_trio_friends", () -> new PaintingVariant(32, 32))'
)

foreach ($Fragment in $RequiredFragments) {
    if (-not $Registry.Contains($Fragment)) {
        throw "V3.9.4 authoritative registry validation failed: $Fragment"
    }
}

$RegistrationCount = ([regex]::Matches($Registry, 'PAINTING_VARIANTS\.register\(')).Count
if ($RegistrationCount -ne 44) {
    throw "V3.9.4 expected exactly 44 painting registrations, found $RegistrationCount."
}

Write-Host "[OK] Authoritative ModPaintingVariants.java copied byte-for-byte." -ForegroundColor Green
Write-Host "[OK] 44 painting variants present." -ForegroundColor Green
Write-Host "[OK] 06_relaxing_on_grass = 48x64 px = 3x4 blocks." -ForegroundColor Green
Write-Host "[OK] compact_03_airsoft_team = 48x32 px = 3x2 blocks." -ForegroundColor Green
Write-Host "[OK] Safe over any partially-applied V3.9.x installer." -ForegroundColor Green
Write-Host "[BUILD] Running full clean build..." -ForegroundColor Cyan

$BuildLog = Join-Path $Root "CUSTOM_PAINTINGS_V3_9_4_BUILD_LAST.txt"
$BuildLogName = Split-Path -Leaf $BuildLog

Push-Location $Root
try {
    & cmd.exe /d /c "gradlew.bat -PdomeFullDev=true clean build --no-daemon > `"$BuildLogName`" 2>&1"
    $Exit = $LASTEXITCODE
}
finally {
    Pop-Location
}

if ($Exit -ne 0) {
    Write-Host "[ERROR] Full build failed." -ForegroundColor Red
    Write-Host "Log: $BuildLog" -ForegroundColor Red
    Write-Host "Backup: $Backup" -ForegroundColor Yellow
    exit $Exit
}

Write-Host ""
Write-Host "[OK] Full clean build succeeded." -ForegroundColor Green
Write-Host "[OK] Custom Paintings V3.9.4 installed." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  .\dev\RUN_DEV_FULL.bat"
Write-Host ""
Write-Host "Test:"
Write-Host "  /give @s domesurvival:memory_painting 32"
exit 0
