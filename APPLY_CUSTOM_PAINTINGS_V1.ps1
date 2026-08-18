$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path $PSScriptRoot).Path
$CheckFiles = @(
  'kubejs\startup_scripts\domesurvival_custom_paintings_startup.js',
  'kubejs\server_scripts\domesurvival_custom_paintings.js',
  'kubejs\assets\domesurvival\textures\item\memory_painting.png',
  'kubejs\data\domesurvival\painting_variant\01_trio_friends.json',
  'kubejs\data\domesurvival\painting_variant\14_mirror_group_selfie.json'
)
foreach ($rel in $CheckFiles) {
  $path = Join-Path $Root $rel
  if (-not (Test-Path -LiteralPath $path)) { throw "Missing required file: $rel" }
}
$VariantCount = (Get-ChildItem -LiteralPath (Join-Path $Root 'kubejs\data\domesurvival\painting_variant') -Filter *.json | Measure-Object).Count
$TextureCount = (Get-ChildItem -LiteralPath (Join-Path $Root 'kubejs\assets\domesurvival\textures\painting') -Filter *.png | Measure-Object).Count
Write-Host '============================================================' -ForegroundColor Cyan
Write-Host 'Dome Survival - CUSTOM PERSONAL PAINTINGS V1' -ForegroundColor Cyan
Write-Host '============================================================' -ForegroundColor Cyan
Write-Host ('[OK] Painting variants: ' + $VariantCount) -ForegroundColor Green
Write-Host ('[OK] Painting textures: ' + $TextureCount) -ForegroundColor Green
Write-Host '[OK] Files are in place.' -ForegroundColor Green
Write-Host '[NEXT] Fully restart Minecraft client/server.' -ForegroundColor Yellow
Write-Host '[USE] Craft 1 обычная картина -> 1 Картина воспоминаний.' -ForegroundColor Yellow
