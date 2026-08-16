@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo ============================================================
echo Dome Survival - BOOTSTRAP EFFECTIVE MODPACK V6.3
echo ============================================================
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0SYNC_FULL_MODPACK.ps1"
exit /b %ERRORLEVEL%
