param(
    [switch]$Apply
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$chapterRoot = Join-Path $PSScriptRoot 'ftbquests\quests\chapters'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$culture = [System.Globalization.CultureInfo]::InvariantCulture

function Get-QuestNodes([string[]]$lines) {
    $nodes = New-Object System.Collections.ArrayList
    $insideQuests = $false
    for ($i = 0; $i -lt $lines.Length; $i++) {
        if ($lines[$i] -eq '  quests: [') {
            $insideQuests = $true
            continue
        }
        if (-not $insideQuests) { continue }
        if ($lines[$i] -eq '  ]') { break }
        if ($lines[$i] -ne '  {') { continue }

        $start = $i
        $end = $i + 1
        while ($end -lt $lines.Length -and $lines[$end] -notmatch '^  \},?$') { $end++ }
        if ($end -ge $lines.Length) { throw "Unclosed top-level quest at line $($start + 1)" }

        $id = $null
        $xLine = -1
        $yLine = -1
        $oldX = 0.0
        $oldY = 0.0
        $deps = @()
        $tag = ''
        $hideLineLine = -1
        for ($j = $start + 1; $j -lt $end; $j++) {
            if ($lines[$j] -match '^    id: "([0-9A-F]+)"') { $id = $Matches[1] }
            elseif ($lines[$j] -match '^    x: (-?[0-9.]+)d$') {
                $xLine = $j
                $oldX = [double]::Parse($Matches[1], $culture)
            }
            elseif ($lines[$j] -match '^    y: (-?[0-9.]+)d$') {
                $yLine = $j
                $oldY = [double]::Parse($Matches[1], $culture)
            }
            elseif ($lines[$j] -match '^    dependencies: \[(.*)\]$') {
                $deps = @([regex]::Matches($Matches[1], '"([0-9A-F]+)"') | ForEach-Object { $_.Groups[1].Value })
            }
            elseif ($lines[$j] -match '^    tags: \["([^"]+)"\]$') {
                $tag = $Matches[1]
            }
            elseif ($lines[$j] -match '^    hide_dependency_lines: (true|false)$') {
                $hideLineLine = $j
            }
        }

        if ($id -and $xLine -ge 0 -and $yLine -ge 0) {
            [void]$nodes.Add([pscustomobject]@{
                Id = $id
                XLine = $xLine
                YLine = $yLine
                OldX = $oldX
                OldY = $oldY
                Dependencies = $deps
                Tag = $tag
                HideLineLine = $hideLineLine
                Depth = 0
                Score = 0.0
            })
        }
        $i = $end
    }
    return @($nodes)
}

function Get-Positions($layers) {
    $positions = @{}
    foreach ($key in @($layers.Keys)) {
        $index = 0
        foreach ($node in @($layers[$key])) {
            $positions[$node.Id] = $index
            $index++
        }
    }
    return $positions
}

function Get-AveragePosition([string[]]$ids, $positions, [double]$fallback) {
    $values = @($ids | Where-Object { $positions.ContainsKey($_) } | ForEach-Object { [double]$positions[$_] })
    if ($values.Count -eq 0) { return $fallback }
    return [double](($values | Measure-Object -Average).Average)
}

$totalNodes = 0
$oldBackwardEdges = 0
$changedFiles = 0

foreach ($file in Get-ChildItem -LiteralPath $chapterRoot -Filter '*.snbt' | Sort-Object Name) {
    $lines = [System.IO.File]::ReadAllLines($file.FullName, [System.Text.Encoding]::UTF8)
    $nodes = @(Get-QuestNodes $lines)
    if ($nodes.Count -eq 0) { continue }

    $byId = @{}
    foreach ($node in $nodes) { $byId[$node.Id] = $node }

    # Longest dependency path establishes clean left-to-right columns.
    for ($pass = 0; $pass -lt $nodes.Count; $pass++) {
        $changed = $false
        foreach ($node in $nodes) {
            $parents = @($node.Dependencies | Where-Object { $byId.ContainsKey($_) })
            if ($parents.Count -eq 0) { continue }
            $candidate = 1 + [int](($parents | ForEach-Object { $byId[$_].Depth } | Measure-Object -Maximum).Maximum)
            if ($candidate -gt $node.Depth) {
                $node.Depth = $candidate
                $changed = $true
            }
        }
        if (-not $changed) { break }
    }

    $layers = @{}
    foreach ($node in $nodes) {
        if (-not $layers.ContainsKey($node.Depth)) { $layers[$node.Depth] = @() }
        $layers[$node.Depth] = @($layers[$node.Depth]) + $node
    }
    foreach ($depth in @($layers.Keys)) {
        $layers[$depth] = @($layers[$depth] | Sort-Object OldY, OldX, Id)
    }

    # Barycentric forward/backward sweeps reduce line crossings while keeping
    # the author's existing vertical order as a stable tie-breaker.
    $maxDepth = [int](($nodes | Measure-Object Depth -Maximum).Maximum)
    for ($sweep = 0; $sweep -lt 6; $sweep++) {
        for ($depth = 1; $depth -le $maxDepth; $depth++) {
            if (-not $layers.ContainsKey($depth)) { continue }
            $positions = Get-Positions $layers
            foreach ($node in @($layers[$depth])) {
                $node.Score = Get-AveragePosition $node.Dependencies $positions $node.OldY
            }
            $layers[$depth] = @($layers[$depth] | Sort-Object Score, OldY, Id)
        }
        for ($depth = $maxDepth - 1; $depth -ge 0; $depth--) {
            if (-not $layers.ContainsKey($depth)) { continue }
            $positions = Get-Positions $layers
            foreach ($node in @($layers[$depth])) {
                $children = @($nodes | Where-Object { $_.Dependencies -contains $node.Id } | ForEach-Object { $_.Id })
                $node.Score = Get-AveragePosition $children $positions $node.OldY
            }
            $layers[$depth] = @($layers[$depth] | Sort-Object Score, OldY, Id)
        }
    }

    foreach ($node in $nodes) {
        foreach ($dependency in $node.Dependencies) {
            if ($byId.ContainsKey($dependency) -and $node.OldY -le $byId[$dependency].OldY) {
                $oldBackwardEdges++
            }
        }
    }

    foreach ($depth in @($layers.Keys)) {
        $layer = @($layers[$depth])
        $center = ($layer.Count - 1) / 2.0
        for ($index = 0; $index -lt $layer.Count; $index++) {
            $node = $layer[$index]
            # FTB Quests reads naturally from top to bottom. Each progression
            # stage occupies one row; parallel support tasks form short side
            # branches instead of extending the book into one huge corridor.
            $newX = ([double]$index - $center) * 3.2
            $newY = [double]$depth * 3.0
            $lines[$node.XLine] = '    x: ' + $newX.ToString('0.0', $culture) + 'd'
            $lines[$node.YLine] = '    y: ' + $newY.ToString('0.0', $culture) + 'd'
        }
    }

    for ($lineIndex = 0; $lineIndex -lt $lines.Length; $lineIndex++) {
        if ($lines[$lineIndex] -match '^  default_hide_dependency_lines:') {
            $lines[$lineIndex] = '  default_hide_dependency_lines: false'
            break
        }
    }

    # Every dependency remains permanently visible. The layered top-to-bottom
    # placement supplies the structure, so players never need to hover a quest
    # merely to understand which step unlocks the next one.
    $lineList = New-Object 'System.Collections.Generic.List[string]'
    $lineList.AddRange([string[]]$lines)
    foreach ($node in @($nodes | Sort-Object YLine -Descending)) {
        $value = 'false'
        if ($node.HideLineLine -ge 0) {
            $lineList[$node.HideLineLine] = "    hide_dependency_lines: $value"
        } else {
            $lineList.Insert($node.YLine + 1, "    hide_dependency_lines: $value")
        }
    }
    $lines = $lineList.ToArray()

    $totalNodes += $nodes.Count
    if ($Apply) {
        [System.IO.File]::WriteAllLines($file.FullName, $lines, $utf8NoBom)
        $changedFiles++
    }
    Write-Host ('[{0}] quests={1}, layers={2}' -f $file.Name, $nodes.Count, ($maxDepth + 1))
}

if ($Apply) {
    Write-Host "[OK] Re-laid $totalNodes quests in $changedFiles chapters."
} else {
    Write-Host "[DRY RUN] $totalNodes quests; $oldBackwardEdges upward/horizontal dependency lines would be removed."
    Write-Host 'Run again with -Apply to write the coordinates.'
}
