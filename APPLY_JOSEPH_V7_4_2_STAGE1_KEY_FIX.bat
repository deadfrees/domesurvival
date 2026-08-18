@echo off
setlocal EnableExtensions
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_JOSEPH_V7_4_2_STAGE1_KEY_FIX.ps1"
exit /b %ERRORLEVEL%
