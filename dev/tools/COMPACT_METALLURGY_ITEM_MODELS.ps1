param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$modelRoot = Join-Path $Project "src\main\resources\assets\domesurvival\models\item"
$utf8NoBom = New-Object Text.UTF8Encoding($false)

$display = @'
{
  "thirdperson_righthand": { "rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.24, 0.24, 0.24] },
  "thirdperson_lefthand": { "rotation": [75, 225, 0], "translation": [0, 2.5, 0], "scale": [0.24, 0.24, 0.24] },
  "firstperson_righthand": { "rotation": [0, 45, 0], "translation": [0, 1.5, 0], "scale": [0.25, 0.25, 0.25] },
  "firstperson_lefthand": { "rotation": [0, 225, 0], "translation": [0, 1.5, 0], "scale": [0.25, 0.25, 0.25] },
  "ground": { "translation": [0, 1.5, 0], "scale": [0.28, 0.28, 0.28] },
  "gui": { "rotation": [28, 225, 0], "translation": [0, -0.5, 0], "scale": [0.46, 0.46, 0.46] },
  "head": { "translation": [0, 9, 0], "scale": [0.3, 0.3, 0.3] },
  "fixed": { "rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.34, 0.34, 0.34] }
}
'@ | ConvertFrom-Json

foreach ($name in @("coke_oven.json", "shaft_furnace.json")) {
    $path = Join-Path $modelRoot $name
    $model = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    if ($null -eq $model.display) {
        $model | Add-Member -MemberType NoteProperty -Name "display" -Value $display
    } else {
        $model.display = $display
    }
    [IO.File]::WriteAllText($path, (($model | ConvertTo-Json -Depth 100) + [Environment]::NewLine), $utf8NoBom)
}

Write-Host "Metallurgy item models compacted."
