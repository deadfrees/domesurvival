param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem

$modsDir = Join-Path $ProjectRoot "run\mods"
$datapackData = Join-Path $ProjectRoot "dev\lastworld\datapack\data"
$compatRoot = Join-Path $datapackData "lcmt"
$jar = Get-ChildItem -LiteralPath $modsDir -Filter "yulari-1.20.1-lostcities.jar" | Select-Object -First 1
if ($null -eq $jar) {
    throw "Yulari JAR was not found in $modsDir. Run dev\SYNC_FULL_MODPACK.ps1 first."
}

$expectedParent = [IO.Path]::GetFullPath($datapackData).TrimEnd('\') + '\'
$resolvedTarget = [IO.Path]::GetFullPath($compatRoot)
if (-not $resolvedTarget.StartsWith($expectedParent, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to replace an unexpected compatibility directory: $resolvedTarget"
}
if (Test-Path -LiteralPath $resolvedTarget) {
    Remove-Item -LiteralPath $resolvedTarget -Recurse -Force
}

$knownNamespaces = @(
    "minecraft", "lostcities", "lcmt", "forge", "c",
    "farmersdelight", "immersiveengineering"
)
$missingNamespacePattern = '(?<![a-z0-9_.-])(?<namespace>[a-z0-9_.-]+):(?<path>[a-z0-9_./-]+)(?:\[[^\]"\r\n]*\])?'

function Get-CompatibleBlock([string]$path) {
    $name = $path.ToLowerInvariant()
    if ($name -match 'trapdoor') { return 'minecraft:iron_trapdoor' }
    if ($name -match 'door') { return 'minecraft:iron_door' }
    if ($name -match 'glass.*pane|pane|window') { return 'minecraft:gray_stained_glass_pane' }
    if ($name -match 'glass') { return 'minecraft:gray_stained_glass' }
    if ($name -match 'stairs?') { return 'minecraft:polished_deepslate_stairs' }
    if ($name -match 'slab') { return 'minecraft:polished_deepslate_slab' }
    if ($name -match 'wall') { return 'minecraft:polished_deepslate_wall' }
    if ($name -match 'fence|bars?|railing|chain') { return 'minecraft:iron_bars' }
    if ($name -match 'lantern|lamp|light|neon') { return 'minecraft:redstone_lamp' }
    if ($name -match 'leaves|hedge|vine|moss|plant|flower') { return 'minecraft:acacia_leaves' }
    if ($name -match 'log|trunk|wood') { return 'minecraft:stripped_acacia_wood' }
    if ($name -match 'bookshelf|shelf|cabinet|desk|table|chair|bench|counter|cupboard|fridge') { return 'minecraft:barrel' }
    if ($name -match 'pipe|duct|beam|girder|metal|steel|iron|machine|industrial') { return 'minecraft:iron_block' }
    if ($name -match 'brick|tile|stone|concrete|asphalt|road|cobble') { return 'minecraft:gray_concrete' }
    return 'minecraft:polished_deepslate'
}

$utf8NoBom = [Text.UTF8Encoding]::new($false)
$archive = [IO.Compression.ZipFile]::OpenRead($jar.FullName)
$written = 0
$script:replaced = 0
$lootContainersRepaired = 0
$dryTerrainReplacements = 0
try {
    foreach ($entry in $archive.Entries) {
        # Some Windows ZIP writers store entry names with backslashes.  ZIP/JAR
        # consumers expect '/', but accepting both here also lets us repair an
        # already repacked development copy instead of silently producing an
        # empty compatibility datapack.
        $entryPath = $entry.FullName.Replace('\', '/')
        $isLcmtData = $entryPath.StartsWith('data/lcmt/', [StringComparison]::OrdinalIgnoreCase)
        $isBorderStyle = $entryPath.Equals('data/lostcities/lostcities/citystyles/citystyle_border.json', [StringComparison]::OrdinalIgnoreCase)
        if ((-not $isLcmtData -and -not $isBorderStyle) -or
            -not $entryPath.EndsWith('.json', [StringComparison]::OrdinalIgnoreCase)) {
            continue
        }
        $stream = $entry.Open()
        $reader = [IO.StreamReader]::new($stream)
        try { $text = $reader.ReadToEnd() } finally { $reader.Dispose(); $stream.Dispose() }

        $replacementsBefore = $script:replaced
        $sanitized = [regex]::Replace($text, $missingNamespacePattern, {
            param($match)
            if ($knownNamespaces -contains $match.Groups['namespace'].Value) { return $match.Value }
            $script:replaced++
            return Get-CompatibleBlock $match.Groups['path'].Value
        })

        # Yulari was authored against a pack that added a non-vanilla
        # "weathering" property to vanilla-looking masonry blocks. Lost
        # Cities parses palette states lazily, so these bad properties only
        # crash the occasional chunk that selects the affected building.
        $sanitized = [regex]::Replace($sanitized, 'minecraft:(?<path>[a-z0-9_./-]+)\[(?<properties>[^\]]+)\]', {
            param($match)
            $block = $match.Groups['path'].Value
            $properties = @($match.Groups['properties'].Value.Split(',') | Where-Object { $_ -notmatch '^weathering=' })
            if ($properties.Count -eq 0) { return "minecraft:$block" }
            return "minecraft:$block[$($properties -join ',')]"
        })

        # LastWorld is deliberately dry. Decorative water embedded in the
        # imported city assets would otherwise create isolated water pockets.
        $sanitized = [regex]::Replace(
            $sanitized,
            '("block"\s*:\s*")minecraft:water(?:\[[^\]]*\])?(\")',
            '$1minecraft:air$2'
        )

        if ($entryPath -match '/lostcities/palettes/') {
            $beforeDryTerrain = $sanitized
            $sanitized = [regex]::Replace(
                $sanitized,
                'minecraft:grass_block(?:\[[^\]]*\])?',
                'minecraft:sand'
            )
            $sanitized = [regex]::Replace(
                $sanitized,
                'minecraft:dirt(?:\[[^\]]*\])?',
                'minecraft:sandstone'
            )
            if ($sanitized -ne $beforeDryTerrain) {
                $dryTerrainReplacements +=
                    ([regex]::Matches($beforeDryTerrain, 'minecraft:grass_block(?:\[[^\]]*\])?')).Count +
                    ([regex]::Matches($beforeDryTerrain, 'minecraft:dirt(?:\[[^\]]*\])?')).Count
            }
        }

        # Lost Cities can assign a loot table only to vanilla randomizable
        # containers. Several Yulari palettes attach loot markers to storage
        # blocks supplied by its original (much larger) modpack. If one of
        # those blocks was replaced with solid masonry, the building generated
        # an empty spot and logged "Error setting loot". Make every marked
        # palette entry a compact, rotatable vanilla barrel instead.
        $repairedLootInEntry = $false
        if ($entryPath -match '/lostcities/palettes/') {
            $paletteAsset = $sanitized | ConvertFrom-Json
            foreach ($paletteEntry in @($paletteAsset.palette)) {
                $hasLoot = $null -ne $paletteEntry.PSObject.Properties['loot']
                $blockName = [string]$paletteEntry.block
                $isVanillaContainer = $blockName -match '^minecraft:(?:chest|trapped_chest|barrel)(?:\[|$)'
                if ($hasLoot -and -not $isVanillaContainer) {
                    $paletteEntry.block = 'minecraft:barrel[facing=up]'
                    $lootContainersRepaired++
                    $repairedLootInEntry = $true
                }
            }
            if ($repairedLootInEntry) {
                $sanitized = $paletteAsset | ConvertTo-Json -Depth 100
            }
        }
        $removedBrokenBuildings = $false
        if ($entryPath.EndsWith('/citystyle_common.json', [StringComparison]::OrdinalIgnoreCase) -or $isBorderStyle) {
            $style = $sanitized | ConvertFrom-Json
            $style.selectors.buildings = @(
                [pscustomobject]@{ factor = 0.30; value = 'lostcities:building1' },
                [pscustomobject]@{ factor = 0.40; value = 'lostcities:building2' },
                [pscustomobject]@{ factor = 0.20; value = 'lostcities:building3' },
                [pscustomobject]@{ factor = 0.20; value = 'lostcities:building4' },
                [pscustomobject]@{ factor = 0.30; value = 'lostcities:building5' },
                [pscustomobject]@{ factor = 0.20; value = 'lostcities:building6' },
                [pscustomobject]@{ factor = 0.20; value = 'lostcities:building7' },
                [pscustomobject]@{ factor = 0.40; value = 'lostcities:building8' }
            )
            $removedBrokenBuildings = $true
            $sanitized = $style | ConvertTo-Json -Depth 100
        }
        $needsOverride = $script:replaced -ne $replacementsBefore -or
            $removedBrokenBuildings -or
            $sanitized -ne $text
        if (-not $needsOverride) { continue }

        $relative = $entryPath.Substring('data/'.Length).Replace('/', '\')
        $target = Join-Path $datapackData $relative
        $targetDir = Split-Path -Parent $target
        New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
        [IO.File]::WriteAllText($target, $sanitized, $utf8NoBom)
        $written++
    }
} finally {
    $archive.Dispose()
}

Write-Host "[OK] Yulari compatibility layer generated."
Write-Host "Overrides: $written JSON files"
Write-Host "Replaced missing block/item references: $script:replaced"
Write-Host "Repaired loot-bearing containers: $lootContainersRepaired"
Write-Host "Replaced non-desert terrain palette entries: $dryTerrainReplacements"
