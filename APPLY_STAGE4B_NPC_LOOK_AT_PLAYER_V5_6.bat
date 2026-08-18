@echo off
setlocal EnableExtensions
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_STAGE4B_NPC_LOOK_AT_PLAYER_V5_6.ps1"
exit /b %ERRORLEVEL%
