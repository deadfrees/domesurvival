@echo off
setlocal EnableExtensions
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_CUSTOM_PAINTINGS_V3_5_LOGGER_COMPILEFIX.ps1"
exit /b %ERRORLEVEL%
