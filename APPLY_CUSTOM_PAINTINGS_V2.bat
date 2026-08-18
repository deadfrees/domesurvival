@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0APPLY_CUSTOM_PAINTINGS_V2.ps1"
if errorlevel 1 (
  echo.
  echo [ERROR] Custom paintings validation failed.
  pause
  exit /b 1
)
echo.
echo [OK] Custom paintings patch is ready.
pause
