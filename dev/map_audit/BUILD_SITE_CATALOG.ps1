[CmdletBinding()]
param(
    [string]$Root = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$auditRoot = Join-Path $Root "dev\map_audit"
$sites = Get-Content -LiteralPath (Join-Path $auditRoot "candidate_sites.json") -Raw -Encoding utf8 | ConvertFrom-Json
$containers = Import-Csv -LiteralPath (Join-Path $auditRoot "output\containers.csv")
$result = foreach ($site in $sites) {
    $nearby = foreach ($container in $containers) {
        $dx = [double]$container.x - [double]$site.x
        $dy = [double]$container.y - [double]$site.y
        $dz = [double]$container.z - [double]$site.z
        $horizontal = [Math]::Sqrt($dx * $dx + $dz * $dz)
        $distance = [Math]::Sqrt($horizontal * $horizontal + $dy * $dy)
        if ($horizontal -le 48 -and [Math]::Abs($dy) -le 32) {
            [pscustomobject]@{
                Distance = $distance
                Horizontal = $horizontal
                X = [int]$container.x
                Y = [int]$container.y
                Z = [int]$container.z
                Id = $container.id
                LootTable = $container.loot_table
                ItemStacks = [int]$container.item_stacks
            }
        }
    }
    $nearby = @($nearby | Sort-Object Distance)
    $within24 = @($nearby | Where-Object { $_.Horizontal -le 32 -and $_.Distance -le 40 })
    $nearest = $nearby | Select-Object -First 1
    [pscustomobject]@{
        id = $site.id
        name = $site.name
        category = $site.category
        x = [int]$site.x
        y = [int]$site.y
        z = [int]$site.z
        containers_32 = $within24.Count
        containers_48 = $nearby.Count
        nearest_distance = if ($nearest) { [Math]::Round($nearest.Distance, 1) } else { "" }
        nearest_x = if ($nearest) { $nearest.X } else { "" }
        nearest_y = if ($nearest) { $nearest.Y } else { "" }
        nearest_z = if ($nearest) { $nearest.Z } else { "" }
        needs_storage_review = ($within24.Count -eq 0)
    }
}

$output = Join-Path $auditRoot "output\site_catalog.csv"
$result | Export-Csv -LiteralPath $output -NoTypeInformation -Encoding utf8

$summary = [ordered]@{
    sites = @($result).Count
    categories = @($result | Group-Object category | Sort-Object Name | ForEach-Object {
        [ordered]@{ category = $_.Name; sites = $_.Count }
    })
    sites_with_container_32 = @($result | Where-Object { -not $_.needs_storage_review }).Count
    sites_needing_storage_review = @($result | Where-Object { $_.needs_storage_review }).Count
}
$summary | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $auditRoot "output\site_catalog_summary.json") -Encoding utf8

Write-Host "Site catalog written: $output" -ForegroundColor Green
Write-Host "Sites: $($summary.sites)"
Write-Host "Need storage review: $($summary.sites_needing_storage_review)"
