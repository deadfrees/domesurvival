@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo ============================================================
echo Dome Survival - FULL MODPACK BOOTSTRAP V6.8 STABLE
echo ============================================================
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0SYNC_FULL_MODPACK.ps1"
exit /b %ERRORLEVEL%
