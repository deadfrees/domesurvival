@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo ============================================================
echo Dome Survival - APPLY V6.0 FULL DEV HOOK
echo ============================================================
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_V6_FULL_DEV_PATCH.ps1"
exit /b %ERRORLEVEL%
