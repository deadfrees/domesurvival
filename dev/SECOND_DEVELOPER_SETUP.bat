@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo ============================================================
echo Dome Survival - SECOND DEVELOPER SETUP V6.5
echo ============================================================
echo.

call "%~dp0RESTORE_ALL_MODS.bat"
if errorlevel 1 exit /b 1

call "%~dp0BOOTSTRAP_FULL_MODPACK.bat"
if errorlevel 1 exit /b 1

if exist "%~dp0APPLY_V6_GITIGNORE.bat" (
    call "%~dp0APPLY_V6_GITIGNORE.bat"
    if errorlevel 1 exit /b 1
)

call "%~dp0APPLY_V6_FULL_DEV_PATCH.bat"
if errorlevel 1 exit /b 1

call "%~dp0PREPARE_FULL_DEV_RUNTIME.bat"
if errorlevel 1 exit /b 1

echo.
echo ============================================================
echo SECOND DEVELOPER ENVIRONMENT READY - V6.5
echo ============================================================
echo.
echo Daily:
echo   dev\UPDATE_AND_RUN_FULL.bat
echo.
echo Full run:
echo   dev\RUN_DEV_FULL.bat
echo.
exit /b 0
