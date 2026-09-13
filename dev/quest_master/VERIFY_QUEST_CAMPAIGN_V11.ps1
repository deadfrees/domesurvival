[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$projectPath = (Resolve-Path -LiteralPath $Project).Path
$masterRoot = Join-Path $projectPath "dev\quest_master\ftbquests\quests\chapters"
$runtimeRoot = Join-Path $projectPath "run\config\ftbquests\quests\chapters"
$registryPath = Join-Path $projectPath "src\main\java\com\wasted\domesurvival\forge\quest\QuestGlobalRegistry.java"
$flagsPath = Join-Path $projectPath "src\main\java\com\wasted\domesurvival\forge\quest\QuestProgressFlags.java"
$errors = [System.Collections.Generic.List[string]]::new()

$chapterFiles = @(Get-ChildItem -LiteralPath $masterRoot -Filter "*.snbt" -File | Sort-Object Name)
$questIds = [System.Collections.Generic.List[string]]::new()
$allObjectIds = [System.Collections.Generic.List[string]]::new()
$dependencies = [System.Collections.Generic.List[string]]::new()

foreach ($file in $chapterFiles) {
    $text = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
    foreach ($match in [regex]::Matches($text, '(?m)^    id: "([0-9A-F]{16})"')) {
        $questIds.Add($match.Groups[1].Value)
    }
    foreach ($match in [regex]::Matches($text, '\bid: "([0-9A-F]{16})"')) {
        $allObjectIds.Add($match.Groups[1].Value)
    }
    foreach ($match in [regex]::Matches($text, '(?m)^    dependencies: \[([^\]]*)\]')) {
        foreach ($dependency in [regex]::Matches($match.Groups[1].Value, '"([0-9A-F]{16})"')) {
            $dependencies.Add($dependency.Groups[1].Value)
        }
    }

    $runtime = Join-Path $runtimeRoot $file.Name
    if (-not (Test-Path -LiteralPath $runtime -PathType Leaf)) {
        $errors.Add("Runtime chapter is missing: $($file.Name)")
    } elseif ((Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash -ne
              (Get-FileHash -LiteralPath $runtime -Algorithm SHA256).Hash) {
        $errors.Add("Runtime chapter differs from master: $($file.Name)")
    }
}

foreach ($duplicate in $allObjectIds | Group-Object | Where-Object Count -gt 1) {
    $errors.Add("Duplicate FTB object ID: $($duplicate.Name)")
}

foreach ($dependency in $dependencies | Sort-Object -Unique) {
    if ($dependency -notin $questIds) {
        $errors.Add("Dangling quest dependency: $dependency")
    }
}

$registryText = Get-Content -LiteralPath $registryPath -Raw -Encoding UTF8
$registeredIds = @([regex]::Matches($registryText, 'new QuestSpec\("([0-9A-F]{16})"') |
    ForEach-Object { $_.Groups[1].Value })
$bridgeFile = Join-Path $masterRoot "11FF60B844BBED5B.snbt"
$bridgeText = Get-Content -LiteralPath $bridgeFile -Raw -Encoding UTF8
$bridgeIds = @([regex]::Matches($bridgeText, '(?m)^    id: "([0-9A-F]{16})"') |
    ForEach-Object { $_.Groups[1].Value })

foreach ($questId in $questIds) {
    if ($questId -notin $bridgeIds -and $questId -notin $registeredIds) {
        $errors.Add("Visible quest is absent from QuestGlobalRegistry: $questId")
    }
}

$flagsText = Get-Content -LiteralPath $flagsPath -Raw -Encoding UTF8
$knownFlags = @([regex]::Matches($flagsText, '"([A-Z][A-Z0-9_]+)"') |
    ForEach-Object { $_.Groups[1].Value })
$questText = ($chapterFiles | ForEach-Object {
    Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8
}) -join "`n"
$usedFlags = @()
$usedFlags += [regex]::Matches($questText, '/domequest gate set ([A-Z0-9_]+)') |
    ForEach-Object { $_.Groups[1].Value }
$usedFlags += [regex]::Matches($questText, '/domequest complete [0-9A-F]{16} (?:normal|milestone|chapter) ([A-Z][A-Z0-9_]+)') |
    ForEach-Object { $_.Groups[1].Value }
foreach ($flag in $usedFlags | Sort-Object -Unique) {
    if ($flag -notin $knownFlags) {
        $errors.Add("Quest references an unknown story flag: $flag")
    }
}

$requiredFragments = @(
    'item: "domesurvival:sand_sieve"',
    'item: "domesurvival:coke_oven"',
    'item: "domesurvival:coal_coke"',
    'item: "domesurvival:shaft_furnace"',
    'item: "domesurvival:steel_ingot"',
    'item: "domesurvival:forming_press"',
    'item: "domesurvival:surface_suit_helmet"',
    'item: "domesurvival:bio_repair_kit"',
    'item: "domesurvival:solar_panel_mk1"',
    'item: "domesurvival:solar_panel_mk2"',
    'item: "domesurvival:solar_panel_mk3"'
)
foreach ($fragment in $requiredFragments) {
    if (-not $questText.Contains($fragment)) {
        $errors.Add("Required gameplay milestone is absent: $fragment")
    }
}

$energyText = Get-Content -LiteralPath (Join-Path $masterRoot "4A2E731D5C9B684F.snbt") -Raw -Encoding UTF8
$industryText = Get-Content -LiteralPath (Join-Path $masterRoot "76CBABB04B110F16.snbt") -Raw -Encoding UTF8
foreach ($solarId in @("71A2B3C4D5E60001", "71A2B3C4D5E60002", "71A2B3C4D5E60003")) {
    if (-not $energyText.Contains("id: `"$solarId`"")) {
        $errors.Add("Solar quest is absent from the energy chapter: $solarId")
    }
    if ($industryText.Contains("id: `"$solarId`"")) {
        $errors.Add("Solar quest is duplicated in the industry chapter: $solarId")
    }
}
if ($industryText.Contains("порции питательной смеси")) {
    $errors.Add("Bioincubator quest still describes nutrient mix as incubation feed")
}
if (-not $industryText.Contains('/domequest gate set HEAVY_INDUSTRY_STARTED')) {
    $errors.Add("Heavy industry side branches remain recipe-locked")
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    throw "Quest campaign v11 verification failed with $($errors.Count) error(s)."
}

Write-Host "Quest campaign v11 verification passed." -ForegroundColor Green
Write-Host "Visible quests: $($questIds.Count - $bridgeIds.Count); technical bridge quests: $($bridgeIds.Count)."
