[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival",
    [string]$Source = "dev\art_reference\coke_oven_large.glb"
)

$ErrorActionPreference = "Stop"
$projectPath = (Resolve-Path -LiteralPath $Project).Path
$sourcePath = if ([IO.Path]::IsPathRooted($Source)) { $Source } else { Join-Path $projectPath $Source }
$sourcePath = (Resolve-Path -LiteralPath $sourcePath).Path
$modelRoot = Join-Path $projectPath "src\main\resources\assets\domesurvival\models\block"
$textureRoot = Join-Path $projectPath "src\main\resources\assets\domesurvival\textures\block\metallurgy"
New-Item -ItemType Directory -Force -Path $modelRoot, $textureRoot | Out-Null

$script:bytes = [IO.File]::ReadAllBytes($sourcePath)
if ([Text.Encoding]::ASCII.GetString($script:bytes, 0, 4) -ne "glTF") { throw "Source is not a binary glTF file" }
$jsonLength = [BitConverter]::ToUInt32($script:bytes, 12)
$jsonText = [Text.Encoding]::UTF8.GetString($script:bytes, 20, $jsonLength).TrimEnd([char]32, [char]0)
$script:gltf = $jsonText | ConvertFrom-Json
$binaryHeader = 20 + $jsonLength
$script:binaryOffset = $binaryHeader + 8
$script:culture = [Globalization.CultureInfo]::InvariantCulture
$script:obj = [Text.StringBuilder]::new()
$script:vertexOffset = 0

function New-Identity {
    return [double[]]@(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1)
}

function Multiply-Matrix([double[]]$a, [double[]]$b) {
    $result = [double[]]::new(16)
    for ($column = 0; $column -lt 4; $column++) {
        for ($row = 0; $row -lt 4; $row++) {
            $value = 0.0
            for ($inner = 0; $inner -lt 4; $inner++) {
                $value += [double]$a[$inner * 4 + $row] * [double]$b[$column * 4 + $inner]
            }
            $result[$column * 4 + $row] = $value
        }
    }
    return $result
}

function Transform-Point([double[]]$matrix, [double[]]$point) {
    return [double[]]@(
        (([double]$matrix[0] * $point[0]) + ([double]$matrix[4] * $point[1]) + ([double]$matrix[8] * $point[2]) + ([double]$matrix[12])),
        (([double]$matrix[1] * $point[0]) + ([double]$matrix[5] * $point[1]) + ([double]$matrix[9] * $point[2]) + ([double]$matrix[13]) + 0.09375),
        (([double]$matrix[2] * $point[0]) + ([double]$matrix[6] * $point[1]) + ([double]$matrix[10] * $point[2]) + ([double]$matrix[14]))
    )
}

function Transform-Normal([double[]]$matrix, [double[]]$normal) {
    $x = [double]$matrix[0] * $normal[0] + [double]$matrix[4] * $normal[1] + [double]$matrix[8] * $normal[2]
    $y = [double]$matrix[1] * $normal[0] + [double]$matrix[5] * $normal[1] + [double]$matrix[9] * $normal[2]
    $z = [double]$matrix[2] * $normal[0] + [double]$matrix[6] * $normal[1] + [double]$matrix[10] * $normal[2]
    $length = [Math]::Max(0.000001, [Math]::Sqrt($x * $x + $y * $y + $z * $z))
    return [double[]]@(($x / $length), ($y / $length), ($z / $length))
}

function Get-Determinant([double[]]$matrix) {
    return (
        $matrix[0] * ($matrix[5] * $matrix[10] - $matrix[9] * $matrix[6]) -
        $matrix[4] * ($matrix[1] * $matrix[10] - $matrix[9] * $matrix[2]) +
        $matrix[8] * ($matrix[1] * $matrix[6] - $matrix[5] * $matrix[2])
    )
}

function Read-Component([int]$offset, [int]$componentType) {
    switch ($componentType) {
        5121 { return [double]$script:bytes[$offset] }
        5123 { return [double][BitConverter]::ToUInt16($script:bytes, $offset) }
        5125 { return [double][BitConverter]::ToUInt32($script:bytes, $offset) }
        5126 { return [double][BitConverter]::ToSingle($script:bytes, $offset) }
        default { throw "Unsupported glTF component type: $componentType" }
    }
}

function Get-Accessor([int]$accessorIndex) {
    $accessor = $script:gltf.accessors[$accessorIndex]
    $view = $script:gltf.bufferViews[[int]$accessor.bufferView]
    $componentCount = switch ($accessor.type) {
        "SCALAR" { 1 }
        "VEC2" { 2 }
        "VEC3" { 3 }
        "VEC4" { 4 }
        default { throw "Unsupported glTF accessor type: $($accessor.type)" }
    }
    $componentSize = switch ([int]$accessor.componentType) {
        5121 { 1 }
        5123 { 2 }
        5125 { 4 }
        5126 { 4 }
        default { throw "Unsupported glTF component type: $($accessor.componentType)" }
    }
    $viewOffset = if ($null -ne $view.byteOffset) { [int]$view.byteOffset } else { 0 }
    $accessorOffset = if ($null -ne $accessor.byteOffset) { [int]$accessor.byteOffset } else { 0 }
    $stride = if ($null -ne $view.byteStride) { [int]$view.byteStride } else { $componentCount * $componentSize }
    $start = $script:binaryOffset + $viewOffset + $accessorOffset
    $values = [System.Collections.Generic.List[object]]::new()
    for ($item = 0; $item -lt [int]$accessor.count; $item++) {
        $tuple = [double[]]::new($componentCount)
        for ($component = 0; $component -lt $componentCount; $component++) {
            $tuple[$component] = Read-Component ($start + $item * $stride + $component * $componentSize) ([int]$accessor.componentType)
        }
        $values.Add($tuple)
    }
    return @($values)
}

function Format-Number([double]$value) {
    return $value.ToString("0.######", $script:culture)
}

function Append-Primitive($primitive, [double[]]$worldMatrix, [string]$name) {
    $material = [int]$primitive.material
    # Materials 2 and 3 are the supplied static fire cards. They are replaced by
    # the animated Minecraft overlay and deliberately omitted from the body OBJ.
    if ($material -ge 2) { return }
    $mode = if ($null -ne $primitive.mode) { [int]$primitive.mode } else { 4 }
    if ($mode -ne 4) { throw "Only triangle primitives are supported" }

    $positions = Get-Accessor ([int]$primitive.attributes.POSITION)
    $normals = Get-Accessor ([int]$primitive.attributes.NORMAL)
    $uvs = Get-Accessor ([int]$primitive.attributes.TEXCOORD_0)
    $indices = Get-Accessor ([int]$primitive.indices)
    $script:obj.AppendLine("o $name") | Out-Null
    $script:obj.AppendLine("usemtl material_$material") | Out-Null
    foreach ($position in $positions) {
        $point = Transform-Point $worldMatrix ([double[]]$position)
        $script:obj.AppendLine("v $(Format-Number $point[0]) $(Format-Number $point[1]) $(Format-Number $point[2])") | Out-Null
    }
    foreach ($uv in $uvs) {
        $script:obj.AppendLine("vt $(Format-Number $uv[0]) $(Format-Number $uv[1])") | Out-Null
    }
    foreach ($normal in $normals) {
        $direction = Transform-Normal $worldMatrix ([double[]]$normal)
        $script:obj.AppendLine("vn $(Format-Number $direction[0]) $(Format-Number $direction[1]) $(Format-Number $direction[2])") | Out-Null
    }

    $reverse = (Get-Determinant $worldMatrix) -lt 0
    for ($index = 0; $index -lt $indices.Count; $index += 3) {
        $a = [int]$indices[$index][0]
        $b = [int]$indices[$index + 1][0]
        $c = [int]$indices[$index + 2][0]
        if ($reverse) { $temporary = $b; $b = $c; $c = $temporary }
        $face = @($a, $b, $c) | ForEach-Object {
            $objIndex = $script:vertexOffset + $_ + 1
            "$objIndex/$objIndex/$objIndex"
        }
        $script:obj.AppendLine("f $($face -join ' ')") | Out-Null
    }
    $script:vertexOffset += $positions.Count
}

function Walk-Node([int]$nodeIndex, [double[]]$parentMatrix) {
    $node = $script:gltf.nodes[$nodeIndex]
    $localMatrix = if ($null -ne $node.matrix) { [double[]]$node.matrix } else { New-Identity }
    $worldMatrix = Multiply-Matrix $parentMatrix $localMatrix
    if ($null -ne $node.mesh) {
        $mesh = $script:gltf.meshes[[int]$node.mesh]
        for ($primitiveIndex = 0; $primitiveIndex -lt $mesh.primitives.Count; $primitiveIndex++) {
            Append-Primitive $mesh.primitives[$primitiveIndex] $worldMatrix "node_${nodeIndex}_mesh_$($node.mesh)_$primitiveIndex"
        }
    }
    if ($null -ne $node.children) {
        foreach ($child in $node.children) { Walk-Node ([int]$child) $worldMatrix }
    }
}

$script:obj.AppendLine("# Converted from the user-supplied Blockbench/Sketchfab GLB") | Out-Null
$script:obj.AppendLine("mtllib large_coke_oven.mtl") | Out-Null
$sceneIndex = if ($null -ne $script:gltf.scene) { [int]$script:gltf.scene } else { 0 }
foreach ($rootNode in $script:gltf.scenes[$sceneIndex].nodes) { Walk-Node ([int]$rootNode) (New-Identity) }
[IO.File]::WriteAllText((Join-Path $modelRoot "large_coke_oven.obj"), $script:obj.ToString(), [Text.UTF8Encoding]::new($false))

$materialText = @"
# Materials extracted from the supplied GLB
newmtl material_0
Ka 1.0 1.0 1.0
Kd 1.0 1.0 1.0
Ks 0.0 0.0 0.0
d 1.0
illum 1
map_Kd domesurvival:block/metallurgy/large_coke_oven_body

newmtl material_1
Ka 1.0 1.0 1.0
Kd 1.0 1.0 1.0
Ks 0.0 0.0 0.0
d 1.0
illum 1
map_Kd domesurvival:block/metallurgy/large_coke_oven_cutout
"@
[IO.File]::WriteAllText((Join-Path $modelRoot "large_coke_oven.mtl"), $materialText, [Text.UTF8Encoding]::new($false))

foreach ($mapping in @(@(0, "large_coke_oven_body.png"), @(1, "large_coke_oven_cutout.png"))) {
    $image = $script:gltf.images[[int]$mapping[0]]
    $view = $script:gltf.bufferViews[[int]$image.bufferView]
    $viewOffset = if ($null -ne $view.byteOffset) { [int]$view.byteOffset } else { 0 }
    $imageBytes = [byte[]]::new([int]$view.byteLength)
    [Array]::Copy($script:bytes, $script:binaryOffset + $viewOffset, $imageBytes, 0, $imageBytes.Length)
    [IO.File]::WriteAllBytes((Join-Path $textureRoot $mapping[1]), $imageBytes)
}

Write-Host "Converted supplied GLB to a 3x3x3 Forge OBJ body ($script:vertexOffset vertices); static fire excluded." -ForegroundColor Green
