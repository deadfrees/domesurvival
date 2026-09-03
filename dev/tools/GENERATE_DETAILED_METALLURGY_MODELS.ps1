[CmdletBinding()]
param([string]$Project = "C:\domesurvival")

$ErrorActionPreference = "Stop"
$projectPath = (Resolve-Path -LiteralPath $Project).Path
$modelRoot = Join-Path $projectPath "src\main\resources\assets\domesurvival\models\block"
$sourceOffPath = Join-Path $modelRoot "copper_furnace.json"
$sourceOnPath = Join-Path $modelRoot "copper_furnace_on.json"
$utf8 = [System.Text.UTF8Encoding]::new($false)

function Clone-Json($value) {
    return ($value | ConvertTo-Json -Depth 100 | ConvertFrom-Json)
}

function Write-Json([string]$path, $value) {
    # These files are generated assets; compact JSON keeps reviews focused on the generator.
    $json = $value | ConvertTo-Json -Depth 100 -Compress
    [System.IO.File]::WriteAllText($path, $json + [Environment]::NewLine, $utf8)
}

function Texture-Map([string]$machine, [bool]$upper) {
    $prefix = "domesurvival:block/metallurgy/detailed/$machine"
    return [ordered]@{
        particle = "${prefix}_body"
        body = "${prefix}_body"
        top = "${prefix}_top"
        bottom = "${prefix}_bottom"
        back = "${prefix}_back"
        rim = "${prefix}_rim"
        iron = "${prefix}_iron"
        door = "${prefix}_door"
        fire_off = "${prefix}_fire_off"
        fire = "${prefix}_fire"
        gauge = "${prefix}_gauge"
        vent = "${prefix}_vent"
        soot = if ($upper) { "domesurvival:block/metallurgy/detailed/input_connector" } else { "${prefix}_soot" }
        output = "domesurvival:block/metallurgy/detailed/output_connector"
    }
}

function Transform-Element($element, [double]$scaleXZ, [double]$scaleY, [double]$offsetY) {
    $copy = Clone-Json $element
    $copy.from = @(
        [Math]::Round(8.0 + (([double]$element.from[0] - 8.0) * $scaleXZ), 3),
        [Math]::Round($offsetY + ([double]$element.from[1] * $scaleY), 3),
        [Math]::Round(8.0 + (([double]$element.from[2] - 8.0) * $scaleXZ), 3)
    )
    $copy.to = @(
        [Math]::Round(8.0 + (([double]$element.to[0] - 8.0) * $scaleXZ), 3),
        [Math]::Round($offsetY + ([double]$element.to[1] * $scaleY), 3),
        [Math]::Round(8.0 + (([double]$element.to[2] - 8.0) * $scaleXZ), 3)
    )
    return $copy
}

function Add-Output-Connector($model) {
    $connector = [pscustomobject][ordered]@{
        from = @(5.25, 0.0, 5.25)
        to = @(10.75, 0.22, 10.75)
        faces = [pscustomobject][ordered]@{
            north = [pscustomobject]@{ texture = "#bottom" }
            east = [pscustomobject]@{ texture = "#bottom" }
            south = [pscustomobject]@{ texture = "#bottom" }
            west = [pscustomobject]@{ texture = "#bottom" }
            down = [pscustomobject]@{ uv = @(5.25, 5.25, 10.75, 10.75); texture = "#output" }
        }
    }
    $model.elements = @($model.elements) + @($connector)
}

function New-Cap-Element([double[]]$from, [double[]]$to, [string]$topTexture) {
    return [pscustomobject][ordered]@{
        from = $from
        to = $to
        faces = [pscustomobject][ordered]@{
            north = [pscustomobject]@{ texture = "#iron" }
            east = [pscustomobject]@{ texture = "#iron" }
            south = [pscustomobject]@{ texture = "#iron" }
            west = [pscustomobject]@{ texture = "#iron" }
            up = [pscustomobject]@{ texture = $topTexture }
            down = [pscustomobject]@{ texture = "#top" }
        }
    }
}

function Add-Embedded-Top-Cap($model, [double]$bodyTop) {
    $min = 1.65; $max = 14.35; $portMin = 5.6; $portMax = 10.4
    $baseY = [Math]::Round($bodyTop - 0.28, 3)
    $cap = @(
        (New-Cap-Element @($min, $baseY, $min) @($max, 16.0, $portMin) "#top"),
        (New-Cap-Element @($min, $baseY, $portMax) @($max, 16.0, $max) "#top"),
        (New-Cap-Element @($min, $baseY, $portMin) @($portMin, 16.0, $portMax) "#top"),
        (New-Cap-Element @($portMax, $baseY, $portMin) @($max, 16.0, $portMax) "#top"),
        (New-Cap-Element @($portMin, $baseY, $portMin) @($portMax, 16.0, $portMax) "#soot")
    )
    $model.elements = @($model.elements) + $cap
}

function Prepare-Front-Elements($model, [string]$machine, [bool]$upper) {
    $result = [System.Collections.Generic.List[object]]::new()
    for ($index = 0; $index -lt $model.elements.Count; $index++) {
        # The original projecting rails and handles caused shader-colored seams.
        # A symmetrical grille is baked into the animated chamber texture instead.
        if ($index -ge 13 -and $index -le 18) { continue }
        if ($upper -and $machine -eq "coke_oven" -and $index -eq 19) { continue }
        $element = Clone-Json $model.elements[$index]
        switch ($index) {
            11 { $element.from[2] = 0.94; $element.to[2] = 0.99 }
            12 { $element.from[2] = 0.86; $element.to[2] = 0.90 }
            19 { $element.from[2] = 0.55; $element.to[2] = 0.85 }
        }
        $result.Add($element)
    }
    return @($result)
}

function Darken-Lower-Base($model) {
    # The copper source uses its brightest iron texture on the floor-level skirt.
    # A dark bottom plate prevents shader/specular packs from turning it into a white strip.
    foreach ($face in $model.elements[1].faces.PSObject.Properties.Value) {
        $face.texture = "#bottom"
    }
    # A single flat plinth reaches the block boundary. The DOWN capability remains
    # functional, but the underside is deliberately not rendered over empty space.
    $model.elements[1].from[1] = 0.0
    $model.elements[1].faces.PSObject.Properties.Remove("down")
}

function New-World-Model($source, [string]$machine, [bool]$upper, [bool]$outputConnector) {
    $model = Clone-Json $source
    $model.credit = "Detailed metallurgy model derived from the DomeSurvival copper furnace"
    $model.textures = [pscustomobject](Texture-Map $machine $upper)
    $model.elements = Prepare-Front-Elements $model $machine $upper
    if ($upper) {
        # Both heads share the shaft-furnace envelope; their panel equipment and palette differ.
        $scaleXZ = 0.85
        $scaleY = 0.91
        $offsetY = -0.25 * $scaleY
        $sourceElements = @($model.elements)
        $model.elements = @($sourceElements | ForEach-Object { Transform-Element $_ $scaleXZ $scaleY $offsetY })
        Add-Embedded-Top-Cap $model ([Math]::Round(16.0 * $scaleY + $offsetY, 3))
    }
    if ($outputConnector) {
        Darken-Lower-Base $model
    }
    return $model
}

function New-Inventory-Model($source, [string]$machine) {
    $base = Clone-Json $source
    $base.elements = Prepare-Front-Elements $base $machine $false
    Darken-Lower-Base $base
    $lower = @($base.elements | ForEach-Object { Transform-Element $_ 1.0 0.49 0.0 })
    $upperBase = Clone-Json $source
    $upperBase.elements = Prepare-Front-Elements $upperBase $machine $true
    $upperSource = @($upperBase.elements)
    $upperXZ = 0.85
    $upperY = 0.45
    $upper = @($upperSource | ForEach-Object { Transform-Element $_ $upperXZ $upperY 8.0 })
    return [pscustomobject][ordered]@{
        credit = "Compact two-stage metallurgy item model"
        ambientocclusion = $true
        gui_light = "side"
        textures = [pscustomobject](Texture-Map $machine $false)
        elements = @($lower + $upper)
    }
}

$off = Get-Content -LiteralPath $sourceOffPath -Raw -Encoding utf8 | ConvertFrom-Json
$on = Get-Content -LiteralPath $sourceOnPath -Raw -Encoding utf8 | ConvertFrom-Json

foreach ($machine in @("coke_oven", "shaft_furnace")) {
    Write-Json (Join-Path $modelRoot "${machine}_lower.json") (New-World-Model $off $machine $false $true)
    Write-Json (Join-Path $modelRoot "${machine}_lower_on.json") (New-World-Model $on $machine $false $true)
    Write-Json (Join-Path $modelRoot "${machine}_upper.json") (New-World-Model $off $machine $true $false)
    Write-Json (Join-Path $modelRoot "${machine}_upper_on.json") (New-World-Model $on $machine $true $false)
    Write-Json (Join-Path $modelRoot "${machine}_item.json") (New-Inventory-Model $off $machine)
}

Write-Host "Generated detailed two-block metallurgy models from the copper furnace geometry." -ForegroundColor Green
