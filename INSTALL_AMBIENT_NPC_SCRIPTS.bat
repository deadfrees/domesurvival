@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if "%~1"=="" (
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0INSTALL_AMBIENT_NPC_SCRIPTS.ps1"
) else (
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0INSTALL_AMBIENT_NPC_SCRIPTS.ps1" -WorldPath "%~1"
)

exit /b %ERRORLEVEL%
