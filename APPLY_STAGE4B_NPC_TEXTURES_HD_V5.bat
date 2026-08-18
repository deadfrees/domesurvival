@echo off
setlocal EnableExtensions
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_STAGE4B_NPC_TEXTURES_HD_V5.ps1"
exit /b %ERRORLEVEL%
