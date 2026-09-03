[CmdletBinding()]
param(
    [string]$Project = "C:\domesurvival"
)

$ErrorActionPreference = "Stop"
$projectPath = (Resolve-Path -LiteralPath $Project).Path
$auditOutput = Join-Path $projectPath "dev\map_audit\output"
$stdout = Join-Path $auditOutput "storage_install_server.out.log"
$stderr = Join-Path $auditOutput "storage_install_server.err.log"
$pidFile = Join-Path $auditOutput "storage_install_server.pid"

foreach ($path in @($stdout, $stderr, $pidFile)) {
    if (Test-Path -LiteralPath $path -PathType Leaf) {
        Remove-Item -LiteralPath $path -Force
    }
}

$arguments = @(
    "/d",
    "/c",
    "call dev\CONFIGURE_JAVA17.bat && gradlew.bat -PdomeFullDev=true runServer --no-daemon --console=plain"
)
$process = Start-Process -FilePath "cmd.exe" `
    -ArgumentList $arguments `
    -WorkingDirectory $projectPath `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr `
    -WindowStyle Hidden `
    -PassThru

$process.Id | Set-Content -LiteralPath $pidFile -Encoding ascii
Write-Host "Storage installation server started: PID $($process.Id)" -ForegroundColor Green
Write-Host "stdout: $stdout"
Write-Host "stderr: $stderr"
