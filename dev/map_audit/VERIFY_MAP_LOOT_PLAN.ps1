[CmdletBinding()]
param(
    [string]$Root = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$auditRoot = Join-Path $Root "dev\map_audit"
$sites = Get-Content -LiteralPath (Join-Path $auditRoot "candidate_sites.json") -Raw -Encoding utf8 | ConvertFrom-Json
$plan = Get-Content -LiteralPath (Join-Path $auditRoot "bio_capsule_placement.json") -Raw -Encoding utf8 | ConvertFrom-Json
$storage = Import-Csv -LiteralPath (Join-Path $auditRoot "output\storage_plan.csv")
$lootConfigPath = Join-Path $Root "src\main\resources\data\domesurvival\bio_module_loot\default.json"
$optionalLootTablePath = Join-Path $Root "src\main\resources\data\domesurvival\loot_tables\chests\bio_module_cache.json"
$globalModifiersPath = Join-Path $Root "src\main\resources\data\forge\loot_modifiers\global_loot_modifiers.json"
$errors = [System.Collections.Generic.List[string]]::new()

$siteById = @{}
foreach ($site in $sites) {
    if ($siteById.ContainsKey($site.id)) {
        $errors.Add("Duplicate site id: $($site.id)")
    }
    $siteById[$site.id] = $site
}

$usedSites = @{}
foreach ($entry in $plan.guaranteed) {
    foreach ($slotName in @("first", "second")) {
        $slot = $entry.$slotName
        if (-not $siteById.ContainsKey($slot.site)) {
            $errors.Add("Unknown site '$($slot.site)' for $($entry.entity)")
            continue
        }
        if ($usedSites.ContainsKey($slot.site)) {
            $errors.Add("More than one guaranteed capsule at '$($slot.site)'")
        }
        $usedSites[$slot.site] = $entry.entity
    }

    if ($siteById.ContainsKey($entry.first.site) -and $siteById.ContainsKey($entry.second.site)) {
        $first = $siteById[$entry.first.site]
        $second = $siteById[$entry.second.site]
        $dx = [double]$first.x - [double]$second.x
        $dz = [double]$first.z - [double]$second.z
        $distance = [Math]::Sqrt($dx * $dx + $dz * $dz)
        if ($distance -lt [double]$plan.rules.minimum_pair_distance_blocks) {
            $errors.Add("Pair for $($entry.entity) is only $([Math]::Round($distance)) blocks apart")
        }
    }
}

$species = @($plan.guaranteed.entity)
if (($species | Sort-Object -Unique).Count -ne $species.Count) {
    $errors.Add("A species occurs more than once in the guaranteed list")
}

foreach ($optional in $plan.optional_random_sites) {
    if (-not $siteById.ContainsKey($optional)) {
        $errors.Add("Unknown optional site: $optional")
    }
    if ($usedSites.ContainsKey($optional)) {
        $errors.Add("Optional site is already occupied by a guaranteed capsule: $optional")
    }
}

$coveredSites = @($usedSites.Keys) + @($plan.optional_random_sites)
foreach ($site in $sites) {
    if ($site.id -notin $coveredSites) {
        $errors.Add("Candidate site is neither guaranteed nor optional: $($site.id)")
    }
}

foreach ($row in $storage) {
    if ($row.status -eq "UNRESOLVED") {
        $errors.Add("Storage placement is unresolved for $($row.site_id)")
    }
}

if (-not (Test-Path -LiteralPath $optionalLootTablePath -PathType Leaf)) {
    $errors.Add("Missing optional biological cache loot table")
} else {
    $optionalLootTable = Get-Content -LiteralPath $optionalLootTablePath -Raw -Encoding utf8 | ConvertFrom-Json
    if ($optionalLootTable.type -ne "minecraft:chest") {
        $errors.Add("Optional biological cache must be a chest loot table")
    }
}

if (-not (Test-Path -LiteralPath $lootConfigPath -PathType Leaf)) {
    $errors.Add("Missing biological loot profile config")
} else {
    $lootConfig = Get-Content -LiteralPath $lootConfigPath -Raw -Encoding utf8 | ConvertFrom-Json
    $optionalProfiles = @($lootConfig.profiles | Where-Object {
        $_.namespace -eq "domesurvival" -and $_.path_contains -contains "bio_module_cache"
    })
    if ($optionalProfiles.Count -ne 1) {
        $errors.Add("Expected exactly one optional biological cache profile")
    } else {
        $profile = $optionalProfiles[0]
        if ([double]$profile.chance -ne 0.40) {
            $errors.Add("Optional cache chance must be 0.40")
        }
        if ([double]$profile.damaged_chance -ne 0.70) {
            $errors.Add("Optional cache damaged chance must be 0.70")
        }
        foreach ($group in @("farm", "lab", "genetic_archive", "nether")) {
            if ($profile.groups -notcontains $group) {
                $errors.Add("Optional cache profile is missing group '$group'")
            }
        }
    }
}

if (-not (Test-Path -LiteralPath $globalModifiersPath -PathType Leaf)) {
    $errors.Add("Missing global loot modifier registry")
} else {
    $globalModifiers = Get-Content -LiteralPath $globalModifiersPath -Raw -Encoding utf8 | ConvertFrom-Json
    if ($globalModifiers.entries -notcontains "domesurvival:bio_module") {
        $errors.Add("Biological global loot modifier is not enabled")
    }
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    throw "Map loot plan verification failed with $($errors.Count) error(s)."
}

Write-Host "Map loot plan verification passed." -ForegroundColor Green
Write-Host "Sites: $($sites.Count)"
Write-Host "Guaranteed species: $($species.Count)"
Write-Host "Guaranteed capsule locations: $($usedSites.Count)"
Write-Host "Optional random locations: $($plan.optional_random_sites.Count)"
