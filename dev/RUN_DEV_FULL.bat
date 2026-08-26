@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0.."

set "ROOT=%CD%"
set "MODS=%ROOT%\run\mods"
set "HOLD=%ROOT%\run\mods_dev_hold_all"
set "LOGDIR=%ROOT%\run\logs"
set "GRADLELOG=%LOGDIR%\FULL_DEV_GRADLE_LAST.txt"
set "RESULT=0"

if not exist "%MODS%" mkdir "%MODS%"
if not exist "%HOLD%" mkdir "%HOLD%"
if not exist "%LOGDIR%" mkdir "%LOGDIR%"

echo ============================================================
echo Dome Survival - FULL DEV V6.8 STABLE
echo ForgeGradle fg.deobf + scoped Mixin/JarJar bridge
echo ============================================================
echo.

call "%~dp0CONFIGURE_JAVA17.bat"
if errorlevel 1 exit /b 1
echo.

call "%~dp0RESTORE_ALL_MODS.bat"
if errorlevel 1 exit /b 1

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0SYNC_FULL_MODPACK.ps1"
if errorlevel 1 (
    echo [ERROR] Effective modpack sync failed.
    exit /b 1
)

call "%~dp0PREPARE_FULL_DEV_RUNTIME.bat"
if errorlevel 1 (
    echo [ERROR] FULL DEV preparation failed.
    exit /b 1
)

echo.
echo [MOD HOLD] Hiding physical production JARs from Forge scanner...
for %%F in ("%MODS%\*.jar") do (
    if exist "%%~fF" move /Y "%%~fF" "%HOLD%\" >nul
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$n=@(Get-ChildItem -LiteralPath '.\run\mods' -Filter '*.jar' -File -ErrorAction SilentlyContinue).Count; Write-Host ('[CHECK] Physical run\mods JAR count = ' + $n); if($n -ne 0){exit 1}"
if errorlevel 1 (
    echo [ERROR] Physical run\mods is not empty.
    set "RESULT=1"
    goto RESTORE
)

if exist "%GRADLELOG%" del /Q "%GRADLELOG%" >nul 2>&1

echo.
echo [1/2] clean build
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "& '.\gradlew.bat' '-PdomeFullDev=true' 'clean' 'build' 2>&1 | Tee-Object -FilePath '.\run\logs\FULL_DEV_GRADLE_LAST.txt'; exit $LASTEXITCODE"
if errorlevel 1 (
    set "RESULT=1"
    goto RESTORE
)

echo.
echo [2/2] runClient
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "& '.\gradlew.bat' '-PdomeFullDev=true' 'runClient' 2>&1 | Tee-Object -FilePath '.\run\logs\FULL_DEV_GRADLE_LAST.txt' -Append; exit $LASTEXITCODE"
set "RESULT=!ERRORLEVEL!"

:RESTORE
echo.
echo ============================================================
echo RESTORING PHYSICAL PRODUCTION MODPACK
echo ============================================================

for %%F in ("%HOLD%\*.jar") do (
    if exist "%%~fF" move /Y "%%~fF" "%MODS%\" >nul
)

echo.
if "!RESULT!"=="0" (
    echo [OK] FULL DEV V6.8 exited normally.
) else (
    echo [ERROR] FULL DEV V6.8 returned code !RESULT!.
    echo.
    echo Logs:
    echo   run\logs\FULL_DEV_GRADLE_LAST.txt
    echo   dev\generated\mixin_srg_bridge_report.txt
)

exit /b !RESULT!
