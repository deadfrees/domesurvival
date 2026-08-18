@echo off
setlocal EnableExtensions
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_JOSEPH_QUESTLINE_V7_1.ps1"
exit /b %ERRORLEVEL%
