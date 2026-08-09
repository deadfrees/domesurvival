$root = Split-Path -Parent $MyInvocation.MyCommand.Path
# This script is intended to be copied/extracted into the project root.
$paths = @(
    '.\src\main\resources\assets\domesurvival\models\block\lanos_decorative.obj',
    '.\src\main\resources\assets\domesurvival\models\block\lanos_decorative.mtl',
    '.\src\main\resources\assets\domesurvival\models\block\lanos_abandoned.obj',
    '.\src\main\resources\assets\domesurvival\models\block\lanos_abandoned.mtl'
)
foreach ($path in $paths) {
    Remove-Item $path -Force -ErrorAction SilentlyContinue
}
Write-Host 'Unused OBJ/MTL Lanos files removed.' -ForegroundColor Green
