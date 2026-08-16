@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0APPLY_V6_GITIGNORE.ps1"
exit /b %ERRORLEVEL%
