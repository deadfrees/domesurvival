$paths = @(
    ".\src\main\resources\assets\domesurvival\models\block\lanos_decorative.obj",
    ".\src\main\resources\assets\domesurvival\models\block\lanos_decorative.mtl",
    ".\src\main\resources\assets\domesurvival\models\block\lanos_abandoned.obj",
    ".\src\main\resources\assets\domesurvival\models\block\lanos_abandoned.mtl"
)
foreach ($path in $paths) {
    Remove-Item $path -Force -ErrorAction SilentlyContinue
}
Write-Host "Old OBJ/MTL Lanos files removed (if they existed)." -ForegroundColor Green
