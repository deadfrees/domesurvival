@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0.."

set "ROOT=%CD%"
set "MODS=%ROOT%\run\mods"
set "HOLD=%ROOT%\run\mods_dev_hold_all_npcs"
set "LOGDIR=%ROOT%\run\logs"
set "GRADLELOG=%LOGDIR%\FULL_DEV_NPCS_GRADLE_LAST.txt"
set "PATCHDIR=%ROOT%\dev\generated\customnpcs_dev"
set "PATCHED=%PATCHDIR%\CustomNPCs-DEV-BRIDGED.jar"
set "REPORT=%PATCHDIR%\customnpcs_mixin_bridge_report.txt"
set "BRIDGE_SRC=%ROOT%\dev\tools\CustomNpcsDevBridge.java"
set "BRIDGE_BIN=%ROOT%\dev\tools\bin_customnpcs"
set "MAPPING=%ROOT%\build\createSrgToMcp\output.srg"
set "RESULT=0"
set "CUSTOMNPCS_JAR="

if not exist "%MODS%" mkdir "%MODS%"
if not exist "%HOLD%" mkdir "%HOLD%"
if not exist "%LOGDIR%" mkdir "%LOGDIR%"
if not exist "%PATCHDIR%" mkdir "%PATCHDIR%"
if not exist "%BRIDGE_BIN%" mkdir "%BRIDGE_BIN%"

echo ============================================================
echo Dome Survival - FULL DEV V6.9 + CustomNPCs Bridge V1.3
echo Full CustomNPCs SRG-to-MojMap bridge; NO ForgeGradle deobf
echo ============================================================
echo.

call "%~dp0CONFIGURE_JAVA17.bat"
if errorlevel 1 exit /b 1
echo.

rem Recover this profile's hold if a previous run was interrupted.
for %%F in ("%HOLD%\*.jar") do (
    if exist "%%~fF" move /Y "%%~fF" "%MODS%\" >nul
)

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

if not exist "%MAPPING%" (
    echo [ERROR] ForgeGradle mapping table is missing:
    echo   %MAPPING%
    exit /b 2
)

echo.
echo [CUSTOMNPCS] Locating source production JAR...
for /f "usebackq delims=" %%F in (`powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$m=@(Get-ChildItem -LiteralPath '.\run\mods' -Filter '*.jar' -File | Where-Object { $_.Name -match '(?i)custom.*npcs|customnpcs' }); if($m.Count -ne 1){Write-Error ('Expected exactly one CustomNPCs JAR, found '+$m.Count); exit 2}; $m[0].FullName"`) do set "CUSTOMNPCS_JAR=%%F"

if not defined CUSTOMNPCS_JAR (
    echo [ERROR] CustomNPCs production JAR was not found uniquely.
    exit /b 3
)

for %%F in ("!CUSTOMNPCS_JAR!") do set "CUSTOMNPCS_NAME=%%~nxF"
echo [CUSTOMNPCS] Source: !CUSTOMNPCS_NAME!

echo.
echo [BRIDGE] Compiling targeted CustomNPCs Mixin bridge...
javac.exe --release 17 -encoding UTF-8 -d "%BRIDGE_BIN%" "%BRIDGE_SRC%"
if errorlevel 1 (
    echo [ERROR] CustomNpcsDevBridge compilation failed.
    exit /b 4
)

del /Q "%PATCHED%" >nul 2>&1
del /Q "%REPORT%" >nul 2>&1

echo [BRIDGE] Patching SRG names in all CustomNPCs classes/resources...
java.exe -cp "%BRIDGE_BIN%" CustomNpcsDevBridge "%MAPPING%" "!CUSTOMNPCS_JAR!" "%PATCHED%" "%REPORT%"
if errorlevel 1 (
    echo [ERROR] CustomNPCs SRG-to-MojMap bridge failed.
    exit /b 5
)

if not exist "%PATCHED%" (
    echo [ERROR] Patched CustomNPCs JAR was not produced.
    exit /b 6
)

echo.
echo [MOD HOLD] Hiding ALL physical production JARs...
for %%F in ("%MODS%\*.jar") do (
    if exist "%%~fF" move /Y "%%~fF" "%HOLD%\" >nul
)

echo [CUSTOMNPCS] Installing targeted DEV-bridged JAR only...
copy /Y "%PATCHED%" "%MODS%\CustomNPCs-DEV-BRIDGED.jar" >nul

powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "$j=@(Get-ChildItem -LiteralPath '.\run\mods' -Filter '*.jar' -File); Write-Host ('[CHECK] Physical run\mods JAR count = '+$j.Count); if($j.Count -ne 1 -or $j[0].Name -ne 'CustomNPCs-DEV-BRIDGED.jar'){Write-Error 'Expected only CustomNPCs-DEV-BRIDGED.jar in run\mods'; exit 1}"
if errorlevel 1 (
    set "RESULT=1"
    goto RESTORE
)

if exist "%GRADLELOG%" del /Q "%GRADLELOG%" >nul 2>&1

echo.
echo [1/2] clean build with JosephScriptCommand enabled
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "& '.\gradlew.bat' '-PdomeFullDev=true' '-PdomeNpcDev=true' 'clean' 'build' 2>&1 | Tee-Object -FilePath '.\run\logs\FULL_DEV_NPCS_GRADLE_LAST.txt'; exit $LASTEXITCODE"
if errorlevel 1 (
    set "RESULT=1"
    goto RESTORE
)

echo.
echo [2/2] runClient
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
  "& '.\gradlew.bat' '-PdomeFullDev=true' '-PdomeNpcDev=true' 'runClient' 2>&1 | Tee-Object -FilePath '.\run\logs\FULL_DEV_NPCS_GRADLE_LAST.txt' -Append; exit $LASTEXITCODE"
set "RESULT=!ERRORLEVEL!"

:RESTORE
echo.
echo ============================================================
echo RESTORING PHYSICAL PRODUCTION MODPACK
echo ============================================================

del /Q "%MODS%\CustomNPCs-DEV-BRIDGED.jar" >nul 2>&1

for %%F in ("%HOLD%\*.jar") do (
    if exist "%%~fF" move /Y "%%~fF" "%MODS%\" >nul
)

echo.
if "!RESULT!"=="0" (
    echo [OK] FULL DEV V6.9 + CustomNPCs V1.3 exited normally.
) else (
    echo [ERROR] FULL DEV V6.9 + CustomNPCs V1.3 returned code !RESULT!.
    echo.
    echo Logs:
    echo   run\logs\FULL_DEV_NPCS_GRADLE_LAST.txt
    echo   dev\generated\customnpcs_dev\customnpcs_mixin_bridge_report.txt
)

exit /b !RESULT!
