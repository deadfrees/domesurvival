@echo off
setlocal EnableExtensions
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_CUSTOM_PAINTINGS_V3_3_TAGKEY_FIX.ps1"
exit /b %ERRORLEVEL%
