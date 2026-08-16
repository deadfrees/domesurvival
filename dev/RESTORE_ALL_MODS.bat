@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

set "MODS=%CD%\run\mods"
set "HOLD=%CD%\run\mods_dev_hold_all"
set "OLD_HOLD=%CD%\run\mods_dev_hold"

if not exist "%MODS%" mkdir "%MODS%"

echo ============================================================
echo Dome Survival - RESTORE PHYSICAL MODPACK
echo ============================================================
echo.

if exist "%OLD_HOLD%" (
    for %%F in ("%OLD_HOLD%\*.jar") do (
        if exist "%%~fF" (
            echo [RESTORE OLD] %%~nxF
            move /Y "%%~fF" "%MODS%\" >nul
        )
    )
)

if exist "%HOLD%" (
    for %%F in ("%HOLD%\*.jar") do (
        if exist "%%~fF" (
            echo [RESTORE] %%~nxF
            move /Y "%%~fF" "%MODS%\" >nul
        )
    )
)

echo.
echo [OK] Physical production mod folder restored.
exit /b 0
