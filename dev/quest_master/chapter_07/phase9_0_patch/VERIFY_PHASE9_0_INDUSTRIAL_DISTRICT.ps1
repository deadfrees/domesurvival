[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$errors = @()
function Fail([string]$Message) { $script:errors += $Message }
function Read-Utf8([string]$Path) { [System.IO.File]::ReadAllText($Path, [System.Text.Encoding]::UTF8) }

$projectPath = (Resolve-Path -LiteralPath $Project).Path
$chapterPath = Join-Path $projectPath "dev\quest_master\ftbquests\quests\chapters\76CBABB04B110F16.snbt"
$registryPath = Join-Path $projectPath "dev\quest_master\chapter_07\CHAPTER7_REGISTRY_V9_0.json"
$themePath = Join-Path $projectPath "src\main\resources\assets\ftbquests\ftb_quests_theme.txt"
$globalPath = Join-Path $projectPath "src\main\java\com\wasted\domesurvival\forge\quest\QuestGlobalRegistry.java"
$eventsPath = Join-Path $projectPath "src\main\java\com\wasted\domesurvival\forge\quest\QuestActionEvents.java"
$backgroundPath = Join-Path $projectPath "src\main\resources\assets\domesurvival\textures\gui\quests\chapter_07_industrial_district.png"
$processedBackgroundPath = Join-Path $projectPath "build\resources\main\assets\domesurvival\textures\gui\quests\chapter_07_industrial_district.png"
$maskRecipePath = Join-Path $projectPath "src\main\resources\data\domesurvival\recipes\oxygen_mask.json"

foreach ($path in @($chapterPath,$registryPath,$themePath,$globalPath,$eventsPath,$backgroundPath,$processedBackgroundPath,$maskRecipePath)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { Fail "Missing required file: $path" }
}
if ($errors.Count -gt 0) { throw ($errors -join [Environment]::NewLine) }

$chapter = Read-Utf8 $chapterPath
$registry = Read-Utf8 $registryPath | ConvertFrom-Json
$theme = Read-Utf8 $themePath
$global = Read-Utf8 $globalPath
$events = Read-Utf8 $eventsPath

$questIds = [regex]::Matches($chapter, '(?m)^    id: "(?<id>[0-9A-F]{16})"$')
if ($questIds.Count -ne 30) { Fail "Expected 30 Chapter 7 quests, found $($questIds.Count)." }
if ([regex]::Matches($chapter, '(?m)^    optional: true$').Count -ne 8) { Fail "Expected 8 optional quests." }
if ([regex]::Matches($chapter, '(?im)type:\s*"(?:checkmark|manual)"').Count -ne 0) { Fail "Manual tasks found." }
if ([regex]::Matches($chapter, '(?ms)rewards:\s*\[\s*\{\s*id:\s*"[0-9A-F]{16}"\s*type:\s*"command"').Count -ne 30) { Fail "Every quest must begin with one technical command reward." }

$optionalIds = @("1EF3AFD5F22CEF3B","73BAFC4EAC03F601","78C53741C6AB7F5B","45555AE92619B033","485AFBB5D3AD03EA","0BC2ABE626556A76","05250A652A93EC4A","780B5E94AE3AA047")
$finalStart = $chapter.IndexOf('    id: "256D235BCCE50263"')
$finalEnd = $chapter.IndexOf('    id: "1EF3AFD5F22CEF3B"')
if ($finalStart -lt 0 -or $finalEnd -lt 0) {
    Fail "Could not isolate the main finale."
} else {
    $finalSegment = $chapter.Substring($finalStart, $finalEnd - $finalStart)
    foreach ($id in $optionalIds) {
        if ($finalSegment.Contains($id)) { Fail "Optional quest $id blocks the main finale." }
    }
}

foreach ($idMatch in $questIds) {
    $id = $idMatch.Groups['id'].Value
    if (-not $global.Contains('"' + $id + '"')) { Fail "Global registry is missing quest $id." }
}
if ([regex]::Matches($global, 'new QuestSpec\(').Count -ne 133) { Fail "Global registry must contain 133 visible campaign quests." }

$actions = @("intro","purifier_placed","purified_water_ready","electrolyzer_placed","oxygen_produced","oxygen_line","filler_placed","tank_filled","oxygen_sortie","finale")
foreach ($action in $actions) {
    $path = Join-Path $projectPath "src\main\resources\data\domesurvival\advancements\quest_actions\ch7_$action.json"
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { Fail "Missing action advancement: ch7_$action" }
}
foreach ($literal in @("CH7_SORTIE_MIN_TICKS = 400L","CH7_SORTIE_MIN_RADIUS = 90.0D","CH7_OXYGEN_LINE_BLOCKS = 6","containsPurifiedWater","hasFullOxygenTank")) {
    if (-not $events.Contains($literal)) { Fail "Server action check is missing: $literal" }
}

if (-not $theme.Contains("[76CBABB04B110F16]")) { Fail "Theme section is missing." }
if (-not $theme.Contains("background: domesurvival:textures/gui/quests/chapter_07_industrial_district.png")) { Fail "Background binding is missing." }
$expectedHash = [string]$registry.background.sha256
$sourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $backgroundPath).Hash
$processedHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $processedBackgroundPath).Hash
if ($sourceHash -ne $expectedHash) { Fail "Background hash does not match registry." }
if ($sourceHash -ne $processedHash) { Fail "Processed background is stale." }

Add-Type -AssemblyName System.Drawing
$image = [System.Drawing.Image]::FromFile($backgroundPath)
try {
    if ($image.Width -ne 1672 -or $image.Height -ne 941) { Fail "Unexpected background resolution: $($image.Width)x$($image.Height)" }
} finally { $image.Dispose() }

$ieJar = Get-ChildItem (Join-Path $projectPath "run\mods") -Filter "ImmersiveEngineering-*.jar" -File | Select-Object -First 1
$mekJar = Get-ChildItem (Join-Path $projectPath "run\mods") -Filter "Mekanism-*.jar" -File | Select-Object -First 1
if ($null -eq $ieJar) { Fail "Immersive Engineering jar is missing." }
if ($null -eq $mekJar) { Fail "Mekanism jar is missing." }
foreach ($item in @("immersiveengineering:hammer","immersiveengineering:fluid_pipe","immersiveengineering:fluid_pump","immersiveengineering:capacitor_lv","mekanism:steel_casing","mekanism:basic_control_circuit","mekanism:basic_mechanical_pipe","mekanism:electrolytic_separator")) {
    if (-not $chapter.Contains($item)) { Fail "Optional mod item is missing from chapter: $item" }
}

$runtimePath = Join-Path $projectPath "run\config\ftbquests\quests\chapters\76CBABB04B110F16.snbt"
if (-not (Test-Path -LiteralPath $runtimePath -PathType Leaf)) {
    Fail "Runtime Chapter 7 is missing."
} elseif ((Get-FileHash -Algorithm SHA256 -LiteralPath $runtimePath).Hash -ne (Get-FileHash -Algorithm SHA256 -LiteralPath $chapterPath).Hash) {
    Fail "Runtime Chapter 7 is not synchronized."
}

$visibleChapterFiles = @("5017105A9984A511.snbt","38F6E366B367B563.snbt","643138B0A36D1017.snbt","5E38036299F32A70.snbt","2D984F68E1AC5B77.snbt","4A2E731D5C9B684F.snbt","76CBABB04B110F16.snbt")
$total = 0
foreach ($file in $visibleChapterFiles) {
    $text = Read-Utf8 (Join-Path $projectPath "dev\quest_master\ftbquests\quests\chapters\$file")
    $total += [regex]::Matches($text, '(?m)^    id: "[0-9A-F]{16}"$').Count
}
if ($total -ne 133) { Fail "Expected 133 visible campaign quests, found $total." }

if ($errors.Count -gt 0) {
    Write-Host "DomeSurvival Phase 9.0 verification: FAILED" -ForegroundColor Red
    foreach ($message in $errors) { Write-Host "  - $message" -ForegroundColor Red }
    exit 1
}

Write-Host "DomeSurvival Phase 9.0 verification: OK" -ForegroundColor Green
Write-Host "Campaign quests: 133"
Write-Host "Chapter 7: 22 main + 8 optional"
Write-Host "Optional integrations: Immersive Engineering, Mekanism"
Write-Host "Automatic Chapter 7 checks: 10"
Write-Host "Background: 1672x941, synchronized"

