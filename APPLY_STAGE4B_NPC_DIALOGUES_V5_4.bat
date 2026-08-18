@echo off
setlocal EnableExtensions
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_STAGE4B_NPC_DIALOGUES_V5_4.ps1"
exit /b %ERRORLEVEL%
