@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo ============================================================
echo Dome Survival - SECOND DEVELOPER SETUP V6.8 STABLE
echo ============================================================
echo.

call "%~dp0RESTORE_ALL_MODS.bat"
if errorlevel 1 exit /b 1

call "%~dp0BOOTSTRAP_FULL_MODPACK.bat"
if errorlevel 1 (
    echo [ERROR] Full physical modpack setup failed.
    exit /b 1
)

call "%~dp0PREPARE_FULL_DEV_RUNTIME.bat"
if errorlevel 1 (
    echo [ERROR] FULL DEV runtime preparation failed.
    exit /b 1
)

echo.
echo ============================================================
echo SECOND DEVELOPER ENVIRONMENT READY
echo ============================================================
echo.
echo Full run:
echo   dev\RUN_DEV_FULL.bat
echo.
echo Daily update + full run:
echo   dev\UPDATE_AND_RUN_FULL.bat
echo.
exit /b 0
