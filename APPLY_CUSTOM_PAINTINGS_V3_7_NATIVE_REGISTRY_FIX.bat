@echo off
setlocal EnableExtensions
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_CUSTOM_PAINTINGS_V3_7_NATIVE_REGISTRY_FIX.ps1"
exit /b %ERRORLEVEL%
