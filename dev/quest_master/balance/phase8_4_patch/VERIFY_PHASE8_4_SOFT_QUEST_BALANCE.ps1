[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$verificationErrors = @()

function Add-ErrorMessage {
    param([string]$Message)
    $script:verificationErrors += $Message
}

function Read-Utf8Text {
    param([string]$Path)
    return [System.IO.File]::ReadAllText($Path, [System.Text.Encoding]::UTF8)
}

function Get-QuestSegment {
    param(
        [string]$ChapterText,
        [string]$QuestId
    )

    $questStartPattern = '(?m)^    id:\s*"' + [regex]::Escape($QuestId) + '"\s*$'
    $questStart = [regex]::Match($ChapterText, $questStartPattern)
    if (-not $questStart.Success) {
        return $null
    }

    $afterStart = $questStart.Index + $questStart.Length
    $remaining = $ChapterText.Substring($afterStart)
    $nextQuest = [regex]::Match($remaining, '(?m)^    id:\s*"[0-9A-F]{16}"\s*$')
    if ($nextQuest.Success) {
        return $ChapterText.Substring($questStart.Index, $afterStart + $nextQuest.Index - $questStart.Index)
    }

    return $ChapterText.Substring($questStart.Index)
}

function Get-ItemTaskCount {
    param(
        [string]$QuestSegment,
        [string]$ItemId
    )

    $tasksMatch = [regex]::Match($QuestSegment, '(?ms)^    tasks:\s*\[(?<tasks>.*?)^    \]')
    if (-not $tasksMatch.Success) {
        return $null
    }

    $taskBlocks = [regex]::Matches($tasksMatch.Groups['tasks'].Value, '(?ms)^      \{(?<task>.*?)^      \}')
    foreach ($taskBlock in $taskBlocks) {
        $taskText = $taskBlock.Groups['task'].Value
        $type = [regex]::Match($taskText, '(?m)^\s+type:\s*"([^"]+)"').Groups[1].Value
        $item = [regex]::Match($taskText, '(?m)^\s+item:\s*"([^"]+)"').Groups[1].Value
        if (($type -eq 'item') -and ($item -eq $ItemId)) {
            $countMatch = [regex]::Match($taskText, '(?m)^\s+count:\s*([0-9]+)L?\s*$')
            if ($countMatch.Success) {
                return [int]$countMatch.Groups[1].Value
            }
            return 1
        }
    }

    return $null
}

$projectPath = (Resolve-Path -LiteralPath $Project).Path
$chapterRoot = Join-Path $projectPath "dev\quest_master\ftbquests\quests\chapters"
$manifestPath = Join-Path $projectPath "dev\quest_master\balance\BALANCE_MANIFEST_V8_4.json"
$actionManifestPath = Join-Path $projectPath "dev\quest_master\ACTION_TASKS_MANIFEST_V3_3.json"
$javaPath = Join-Path $projectPath "src\main\java\com\wasted\domesurvival\forge\quest\QuestActionEvents.java"
$themePath = Join-Path $projectPath "src\main\resources\assets\ftbquests\ftb_quests_theme.txt"
$backgroundManifestPath = Join-Path $projectPath "dev\quest_master\visual\QUEST_BACKGROUND_MANIFEST_V8_3.json"

$requiredPaths = @($manifestPath, $actionManifestPath, $javaPath, $themePath, $backgroundManifestPath)
foreach ($requiredPath in $requiredPaths) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        Add-ErrorMessage "Required file is missing: $requiredPath"
    }
}

if ($verificationErrors.Count -gt 0) {
    throw ($verificationErrors -join [Environment]::NewLine)
}

$manifest = Read-Utf8Text -Path $manifestPath | ConvertFrom-Json
$chapterDefinitions = @(
    @{ Id = "5017105A9984A511"; File = "5017105A9984A511.snbt"; Quests = 14 },
    @{ Id = "38F6E366B367B563"; File = "38F6E366B367B563.snbt"; Quests = 17 },
    @{ Id = "643138B0A36D1017"; File = "643138B0A36D1017.snbt"; Quests = 17 },
    @{ Id = "5E38036299F32A70"; File = "5E38036299F32A70.snbt"; Quests = 15 },
    @{ Id = "2D984F68E1AC5B77"; File = "2D984F68E1AC5B77.snbt"; Quests = 20 },
    @{ Id = "4A2E731D5C9B684F"; File = "4A2E731D5C9B684F.snbt"; Quests = 20 }
)

$chapterTexts = @{}
$allChapterText = ""
$allQuestIds = @()
$totalQuestCount = 0
foreach ($chapter in $chapterDefinitions) {
    $chapterPath = Join-Path $chapterRoot $chapter.File
    if (-not (Test-Path -LiteralPath $chapterPath -PathType Leaf)) {
        Add-ErrorMessage "Chapter file is missing: $($chapter.File)"
        continue
    }

    $chapterText = Read-Utf8Text -Path $chapterPath
    $chapterTexts[$chapter.Id] = $chapterText
    $allChapterText += $chapterText + [Environment]::NewLine

    $ids = [regex]::Matches($chapterText, '(?m)^    id:\s*"(?<id>[0-9A-F]{16})"\s*$')
    $questCount = $ids.Count
    $totalQuestCount += $questCount
    foreach ($idMatch in $ids) {
        $allQuestIds += $idMatch.Groups['id'].Value
    }

    if ($questCount -ne $chapter.Quests) {
        Add-ErrorMessage "Quest count mismatch in $($chapter.File): expected $($chapter.Quests), found $questCount."
    }
}

if ($totalQuestCount -ne [int]$manifest.campaign_quests_total) {
    Add-ErrorMessage "Campaign quest count mismatch: expected $($manifest.campaign_quests_total), found $totalQuestCount."
}
if (@($allQuestIds | Sort-Object -Unique).Count -ne $totalQuestCount) {
    Add-ErrorMessage "Duplicate quest IDs found in the six visible campaign chapters."
}
if ([regex]::Matches($allChapterText, '(?im)type:\s*"(?:checkmark|manual)"').Count -ne 0) {
    Add-ErrorMessage "Manual checkmark tasks were found."
}
if ([regex]::Matches($allChapterText, '(?i)unknown|placeholder|missingno|minecraft:barrier').Count -ne 0) {
    Add-ErrorMessage "Unknown or placeholder references were found."
}

$energyText = $chapterTexts['4A2E731D5C9B684F']
$chapterTitleMatch = [regex]::Match($energyText, '(?m)^  title:\s*"\{\\"text\\":\\"(?<title>.*?)\\",')
$introSegment = Get-QuestSegment -ChapterText $energyText -QuestId '613FF462EA9C75DC'
$introTitleMatch = [regex]::Match($introSegment, '(?m)^    title:\s*"(?<title>[^"]+)"\s*$')
if ((-not $chapterTitleMatch.Success) -or (-not $introTitleMatch.Success)) {
    Add-ErrorMessage "The energy chapter or intro title could not be parsed."
}
elseif (($chapterTitleMatch.Groups['title'].Value.Contains('5')) -or
        ($chapterTitleMatch.Groups['title'].Value -ne $introTitleMatch.Groups['title'].Value)) {
    Add-ErrorMessage "The energy chapter title still has a numeric prefix or differs from its intro title."
}

if ($manifest.item_requirements.Count -ne [int]$manifest.changed_item_task_lines) {
    Add-ErrorMessage "Balance manifest item-line count is inconsistent."
}
if ($manifest.action_thresholds.Count -ne [int]$manifest.changed_action_thresholds) {
    Add-ErrorMessage "Balance manifest action-threshold count is inconsistent."
}
$changedQuestCount = @($manifest.item_requirements.quest_id | Sort-Object -Unique).Count
if ($changedQuestCount -ne [int]$manifest.changed_item_quests) {
    Add-ErrorMessage "Balance manifest changed-quest count is inconsistent."
}

foreach ($requirement in $manifest.item_requirements) {
    $chapterText = $chapterTexts[[string]$requirement.chapter_id]
    if ($null -eq $chapterText) {
        Add-ErrorMessage "Requirement references a missing chapter: $($requirement.chapter_id)."
        continue
    }

    $questSegment = Get-QuestSegment -ChapterText $chapterText -QuestId ([string]$requirement.quest_id)
    if ($null -eq $questSegment) {
        Add-ErrorMessage "Requirement references a missing quest: $($requirement.quest_id)."
        continue
    }

    $actualCount = Get-ItemTaskCount -QuestSegment $questSegment -ItemId ([string]$requirement.item)
    if ($null -eq $actualCount) {
        Add-ErrorMessage "Item task is missing: quest $($requirement.quest_id), item $($requirement.item)."
        continue
    }
    if ($actualCount -ne [int]$requirement.new) {
        Add-ErrorMessage "Item count mismatch: quest $($requirement.quest_id), item $($requirement.item), expected $($requirement.new), found $actualCount."
    }

    $descriptionMatch = [regex]::Match($questSegment, '(?ms)^    description:\s*\[(?<description>.*?)^    \]\s*\r?\n    tasks:')
    $newCountPattern = '(?<![0-9])' + [regex]::Escape([string]$requirement.new) + '(?![0-9])'
    if ((-not $descriptionMatch.Success) -or ($descriptionMatch.Groups['description'].Value -notmatch $newCountPattern)) {
        Add-ErrorMessage "Description does not show the new count $($requirement.new): quest $($requirement.quest_id)."
    }
}

$actionDescriptionChecks = @(
    @{ Chapter = '5017105A9984A511'; Quest = '63EB53B2B8F791B2'; Required = @(2); Forbidden = @(1) },
    @{ Chapter = '5017105A9984A511'; Quest = '50DA62AD2BA7A7B2'; Required = @(2); Forbidden = @(1) },
    @{ Chapter = '38F6E366B367B563'; Quest = '3E0DBA929C2D708C'; Required = @(6, 10); Forbidden = @(4, 8) },
    @{ Chapter = '643138B0A36D1017'; Quest = '68986BCB1E8DB7B7'; Required = @(10, 20); Forbidden = @(8) },
    @{ Chapter = '643138B0A36D1017'; Quest = '706EFE49CEFC5DF3'; Required = @(5); Forbidden = @(4) },
    @{ Chapter = '643138B0A36D1017'; Quest = '27AACD200A156F63'; Required = @(18); Forbidden = @(20) },
    @{ Chapter = '643138B0A36D1017'; Quest = '1403CDFAA5692780'; Required = @(6, 18); Forbidden = @(4, 20) },
    @{ Chapter = '643138B0A36D1017'; Quest = '4485BFC51558EF4D'; Required = @(20); Forbidden = @(16) },
    @{ Chapter = '5E38036299F32A70'; Quest = '390DD88E4D110AFD'; Required = @(3); Forbidden = @(2) },
    @{ Chapter = '5E38036299F32A70'; Quest = '502616EC28341C00'; Required = @(5); Forbidden = @(4) },
    @{ Chapter = '2D984F68E1AC5B77'; Quest = '5A8D75EB2724CC12'; Required = @(5); Forbidden = @(4) },
    @{ Chapter = '4A2E731D5C9B684F'; Quest = '3CE842358BCF4A85'; Required = @(4); Forbidden = @(3) },
    @{ Chapter = '4A2E731D5C9B684F'; Quest = '0110D171C4CD09E3'; Required = @(5); Forbidden = @(4) }
)
foreach ($check in $actionDescriptionChecks) {
    $segment = Get-QuestSegment -ChapterText $chapterTexts[$check.Chapter] -QuestId $check.Quest
    $descriptionMatch = [regex]::Match($segment, '(?ms)^    description:\s*\[(?<description>.*?)^    \]\s*\r?\n    tasks:')
    if (-not $descriptionMatch.Success) {
        Add-ErrorMessage "Action description is missing: quest $($check.Quest)."
        continue
    }
    $description = $descriptionMatch.Groups['description'].Value
    foreach ($number in $check.Required) {
        if ($description -notmatch ('(?<![0-9])' + $number + '(?![0-9])')) {
            Add-ErrorMessage "Action description is missing new value ${number}: quest $($check.Quest)."
        }
    }
    foreach ($number in $check.Forbidden) {
        if ($description -match ('(?<![0-9])' + $number + '(?![0-9])')) {
            Add-ErrorMessage "Action description still contains old value ${number}: quest $($check.Quest)."
        }
    }
}

$javaText = Read-Utf8Text -Path $javaPath
$requiredJavaLiterals = @(
    "private static final double CH2_NEAR_MIN_RADIUS = 60.0D;",
    "private static final long CH2_RETURN_MAX_TICKS = 360L;",
    "private static final long CH2_CONTROLLED_MIN_TICKS = 120L;",
    'hasAtLeast(player, "minecraft:bread", 6)',
    'hasAtLeast(player, "minecraft:torch", 10)',
    "if (count >= 5)",
    "if (beds >= 3)",
    "if (lights >= 5)",
    "if (links >= 4)",
    "if (lamps >= 5)",
    "CH4_LIGHTS.getOrDefault(id, 0) >= 5",
    "CH3_BEDS.getOrDefault(id, 0) >= 3",
    "CH3_LIGHTS.getOrDefault(id, 0) >= 5",
    'hasAtLeast(player, "minecraft:sand", 20)'
)
foreach ($literal in $requiredJavaLiterals) {
    if (-not $javaText.Contains($literal)) {
        Add-ErrorMessage "Server threshold is missing: $literal"
    }
}
if ([regex]::Matches($javaText, [regex]::Escape('if (ticks >= 40)')).Count -ne 2) {
    Add-ErrorMessage "Expected two 2-second Chapter 0 exposure thresholds."
}
if ([regex]::Matches($javaText, [regex]::Escape('if (lights >= 5)')).Count -ne 2) {
    Add-ErrorMessage "Expected Chapter 3 and Chapter 4 five-light thresholds."
}

$forbiddenJavaLiterals = @(
    "CH2_NEAR_MIN_RADIUS = 58.0D",
    "CH2_RETURN_MAX_TICKS = 400L",
    "CH2_CONTROLLED_MIN_TICKS = 80L",
    'hasAtLeast(player, "minecraft:bread", 4)',
    'hasAtLeast(player, "minecraft:torch", 8)',
    'hasAtLeast(player, "minecraft:sand", 16)'
)
foreach ($literal in $forbiddenJavaLiterals) {
    if ($javaText.Contains($literal)) {
        Add-ErrorMessage "Old server threshold remains: $literal"
    }
}

$actionManifest = Read-Utf8Text -Path $actionManifestPath | ConvertFrom-Json
$solarAction = $actionManifest.action_advancements | Where-Object quest_id -eq '63EB53B2B8F791B2'
$shadeAction = $actionManifest.action_advancements | Where-Object quest_id -eq '50DA62AD2BA7A7B2'
$readinessAction = $actionManifest.action_advancements | Where-Object quest_id -eq '3E0DBA929C2D708C'
if (($solarAction.task_title -notmatch '~2') -or ($shadeAction.task_title -notmatch '~2')) {
    Add-ErrorMessage "Chapter 0 action-manifest exposure titles are stale."
}
if ($readinessAction.task_title -notmatch '6.*10') {
    Add-ErrorMessage "Chapter 1 readiness title is stale in the action manifest."
}

$runtimeQuestDirectory = Join-Path $projectPath "run\config\ftbquests\quests\chapters"
if (Test-Path -LiteralPath $runtimeQuestDirectory -PathType Container) {
    foreach ($chapter in $chapterDefinitions) {
        $masterPath = Join-Path $chapterRoot $chapter.File
        $runtimePath = Join-Path $runtimeQuestDirectory $chapter.File
        if (-not (Test-Path -LiteralPath $runtimePath -PathType Leaf)) {
            Add-ErrorMessage "Runtime chapter is missing: $($chapter.File)"
            continue
        }
        if ((Get-FileHash -Algorithm SHA256 -LiteralPath $masterPath).Hash -ne (Get-FileHash -Algorithm SHA256 -LiteralPath $runtimePath).Hash) {
            Add-ErrorMessage "Runtime chapter is not synchronized: $($chapter.File)"
        }
    }
}

$themeText = Read-Utf8Text -Path $themePath
$backgroundManifest = Read-Utf8Text -Path $backgroundManifestPath | ConvertFrom-Json
Add-Type -AssemblyName System.Drawing
$backgroundHashes = @()
$backgroundQuality = @()
foreach ($background in $backgroundManifest.backgrounds) {
    $resourceRelative = ([string]$background.resource).Substring("domesurvival:".Length).Replace('/', '\')
    $sourcePath = Join-Path $projectPath ("src\main\resources\assets\domesurvival\" + $resourceRelative)
    $processedPath = Join-Path $projectPath ("build\resources\main\assets\domesurvival\" + $resourceRelative)
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        Add-ErrorMessage "Background source is missing: $($background.resource)"
        continue
    }
    if (-not (Test-Path -LiteralPath $processedPath -PathType Leaf)) {
        Add-ErrorMessage "Processed background is missing: $($background.resource)"
        continue
    }

    $sourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourcePath).Hash
    $processedHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $processedPath).Hash
    $backgroundHashes += $sourceHash
    if ($sourceHash -ne ([string]$background.sha256).ToUpperInvariant()) {
        Add-ErrorMessage "Background hash does not match its manifest: $($background.resource)"
    }
    if ($sourceHash -ne $processedHash) {
        Add-ErrorMessage "Processed background is stale: $($background.resource)"
    }

    $image = [System.Drawing.Image]::FromFile($sourcePath)
    try {
        if (($image.Width -lt 1600) -or ($image.Height -lt 900)) {
            Add-ErrorMessage "Background resolution is too low: $($background.resource) $($image.Width)x$($image.Height)"
        }
        $backgroundQuality += "$($background.title)=$($image.Width)x$($image.Height)"
    }
    finally {
        $image.Dispose()
    }

    if (-not $themeText.Contains("[$($background.chapter_id)]")) {
        Add-ErrorMessage "Theme chapter section is missing: $($background.chapter_id)"
    }
    if (-not $themeText.Contains("background: $($background.resource)")) {
        Add-ErrorMessage "Theme background binding is missing: $($background.resource)"
    }
}
if (@($backgroundHashes | Sort-Object -Unique).Count -ne 6) {
    Add-ErrorMessage "The six chapter backgrounds are not all unique."
}

if ($verificationErrors.Count -gt 0) {
    Write-Host "DomeSurvival Phase 8.4 verification: FAILED" -ForegroundColor Red
    foreach ($message in $verificationErrors) {
        Write-Host "  - $message" -ForegroundColor Red
    }
    exit 1
}

Write-Host "DomeSurvival Phase 8.4 verification: OK" -ForegroundColor Green
Write-Host "Campaign quests: $totalQuestCount"
Write-Host "Changed item quests: $changedQuestCount"
Write-Host "Changed item task lines: $($manifest.item_requirements.Count)"
Write-Host "Changed action thresholds: $($manifest.action_thresholds.Count)"
Write-Host "Manual checkmarks: 0"
Write-Host "Visible Chapter 5 numeric prefix: 0"
Write-Host "Runtime quest copies: synchronized"
Write-Host "High-detail unique backgrounds: $(@($backgroundHashes | Sort-Object -Unique).Count)"
foreach ($qualityRow in $backgroundQuality) {
    Write-Host "  $qualityRow"
}
