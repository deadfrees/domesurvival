@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo ============================================================
echo Dome Survival - PREPARE FULL DEV V6.8
echo Git-ready JarJar-aware Mixin SRG Bridge
echo ============================================================
echo.

set "BRIDGE_SRC=%CD%\dev\tools\MixinSrgBridge.java"
set "BRIDGE_BIN=%CD%\dev\tools\bin"

if not exist "%BRIDGE_SRC%" (
    echo [ERROR] MixinSrgBridge.java not found:
    echo   %BRIDGE_SRC%
    exit /b 1
)

where javac.exe >nul 2>&1
if errorlevel 1 (
    echo [ERROR] javac.exe was not found in PATH.
    echo FULL DEV requires a Java 17 JDK, not only a JRE.
    echo.
    echo Current JAVA_HOME:
    echo   %JAVA_HOME%
    exit /b 1
)

if not exist "%BRIDGE_BIN%" mkdir "%BRIDGE_BIN%"

echo [0/4] Compile MixinSrgBridge with Java 17
javac.exe --release 17 -encoding UTF-8 -d "%BRIDGE_BIN%" "%BRIDGE_SRC%"
if errorlevel 1 (
    echo [ERROR] MixinSrgBridge compilation failed.
    exit /b 1
)

if not exist "%BRIDGE_BIN%\MixinSrgBridge.class" (
    echo [ERROR] MixinSrgBridge.class was not produced.
    exit /b 1
)

echo [OK] MixinSrgBridge compiled locally.
echo.

echo [1/4] ForgeGradle mapping table
call gradlew.bat createSrgToMcp
if errorlevel 1 exit /b 1

if not exist "build\createSrgToMcp\output.srg" (
    echo [ERROR] build\createSrgToMcp\output.srg was not created.
    exit /b 1
)

echo.
echo [2/4] Rebuild/check FULL DEV cache
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0PREPARE_FULL_DEOBF_CACHE.ps1"
if errorlevel 1 exit /b 1

echo.
echo [3/4] Validate JarJar-aware bridge
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0VALIDATE_FULL_DEOBF_CACHE.ps1"
if errorlevel 1 exit /b 1

echo.
echo [4/4] FULL DEV runtime preparation complete
echo [OK] Git-ready FULL DEV V6.8 prepared.
exit /b 0
