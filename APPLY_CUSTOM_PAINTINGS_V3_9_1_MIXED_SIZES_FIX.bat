@echo off
setlocal EnableExtensions
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_CUSTOM_PAINTINGS_V3_9_1_MIXED_SIZES_FIX.ps1"
exit /b %ERRORLEVEL%
