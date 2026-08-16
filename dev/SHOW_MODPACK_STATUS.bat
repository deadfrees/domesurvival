@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0SHOW_MODPACK_STATUS.ps1"
exit /b %ERRORLEVEL%
