@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_STAGE4B_NPC_SKINS_V4.ps1"
exit /b %ERRORLEVEL%
