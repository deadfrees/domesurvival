[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$Path)

$ErrorActionPreference = "Stop"
$bytes = [System.IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $Path).Path)
$jsonLength = [BitConverter]::ToUInt32($bytes, 12)
$jsonText = [Text.Encoding]::UTF8.GetString($bytes, 20, $jsonLength).TrimEnd([char]32, [char]0)
$script:gltf = $jsonText | ConvertFrom-Json
$script:globalMin = [double[]]@(1e9, 1e9, 1e9)
$script:globalMax = [double[]]@(-1e9, -1e9, -1e9)
$script:materialBounds = @{}

function New-Identity {
    return [double[]]@(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1)
}

function Multiply-Matrix([double[]]$a, [double[]]$b) {
    $result = [double[]]::new(16)
    for ($column = 0; $column -lt 4; $column++) {
        for ($row = 0; $row -lt 4; $row++) {
            $value = 0.0
            for ($inner = 0; $inner -lt 4; $inner++) {
                $value = [double]$value + (
                    [double]$a[$inner * 4 + $row] * [double]$b[$column * 4 + $inner]
                )
            }
            $result[$column * 4 + $row] = $value
        }
    }
    return $result
}

function Transform-Point([double[]]$matrix, [double]$x, [double]$y, [double]$z) {
    return [double[]]@(
        (([double]$matrix[0] * $x) + ([double]$matrix[4] * $y) + ([double]$matrix[8] * $z) + ([double]$matrix[12])),
        (([double]$matrix[1] * $x) + ([double]$matrix[5] * $y) + ([double]$matrix[9] * $z) + ([double]$matrix[13])),
        (([double]$matrix[2] * $x) + ([double]$matrix[6] * $y) + ([double]$matrix[10] * $z) + ([double]$matrix[14]))
    )
}

function Expand-Bounds([double[]]$point, [double[]]$minimum, [double[]]$maximum) {
    for ($axis = 0; $axis -lt 3; $axis++) {
        $minimum[$axis] = [Math]::Min($minimum[$axis], $point[$axis])
        $maximum[$axis] = [Math]::Max($maximum[$axis], $point[$axis])
    }
}

function Walk-Node([int]$nodeIndex, [double[]]$parentMatrix) {
    $node = $script:gltf.nodes[$nodeIndex]
    $localMatrix = if ($null -ne $node.matrix) { [double[]]$node.matrix } else { New-Identity }
    $worldMatrix = Multiply-Matrix $parentMatrix $localMatrix

    if ($null -ne $node.mesh) {
        $mesh = $script:gltf.meshes[[int]$node.mesh]
        foreach ($primitive in $mesh.primitives) {
            $accessor = $script:gltf.accessors[[int]$primitive.attributes.POSITION]
            $materialKey = [string]$primitive.material
            if (-not $script:materialBounds.ContainsKey($materialKey)) {
                $script:materialBounds[$materialKey] = @(
                    [double[]]@(1e9, 1e9, 1e9),
                    [double[]]@(-1e9, -1e9, -1e9)
                )
            }
            foreach ($x in @([double]$accessor.min[0], [double]$accessor.max[0])) {
                foreach ($y in @([double]$accessor.min[1], [double]$accessor.max[1])) {
                    foreach ($z in @([double]$accessor.min[2], [double]$accessor.max[2])) {
                        $point = Transform-Point $worldMatrix $x $y $z
                        Expand-Bounds $point $script:globalMin $script:globalMax
                        Expand-Bounds $point $script:materialBounds[$materialKey][0] $script:materialBounds[$materialKey][1]
                    }
                }
            }
        }
    }

    if ($null -ne $node.children) {
        foreach ($child in $node.children) {
            Walk-Node ([int]$child) $worldMatrix
        }
    }
}

function Format-Vector([double[]]$vector) {
    $culture = [Globalization.CultureInfo]::InvariantCulture
    return ($vector | ForEach-Object { $_.ToString("0.####", $culture) }) -join ","
}

$sceneIndex = if ($null -ne $script:gltf.scene) { [int]$script:gltf.scene } else { 0 }
foreach ($rootNode in $script:gltf.scenes[$sceneIndex].nodes) {
    Walk-Node ([int]$rootNode) (New-Identity)
}

$size = [double[]]@(0, 0, 0)
for ($axis = 0; $axis -lt 3; $axis++) { $size[$axis] = $script:globalMax[$axis] - $script:globalMin[$axis] }
Write-Host "WORLD min=$(Format-Vector $script:globalMin) max=$(Format-Vector $script:globalMax) size=$(Format-Vector $size)"
foreach ($key in ($script:materialBounds.Keys | Sort-Object)) {
    $bounds = $script:materialBounds[$key]
    $materialSize = [double[]]@(0, 0, 0)
    for ($axis = 0; $axis -lt 3; $axis++) { $materialSize[$axis] = $bounds[1][$axis] - $bounds[0][$axis] }
    Write-Host "MATERIAL[$key] min=$(Format-Vector $bounds[0]) max=$(Format-Vector $bounds[1]) size=$(Format-Vector $materialSize)"
}
