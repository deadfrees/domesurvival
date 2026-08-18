@echo off
setlocal EnableExtensions
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_JOSEPH_V7_5_1_PROGRESSION_BALANCE.ps1"
exit /b %ERRORLEVEL%
