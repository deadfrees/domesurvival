@echo off
setlocal EnableExtensions

set "DOME_JAVA17="
for /f "usebackq delims=" %%J in (`powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0RESOLVE_JAVA17.ps1"`) do set "DOME_JAVA17=%%J"

if not defined DOME_JAVA17 (
    echo [ERROR] Java 17 JDK was not found.
    echo Install JDK 17 or set JAVA_HOME to its installation folder.
    exit /b 1
)

endlocal & set "JAVA_HOME=%DOME_JAVA17%" & set "PATH=%DOME_JAVA17%\bin;%PATH%"
echo [JAVA] Using %JAVA_HOME%
"%JAVA_HOME%\bin\java.exe" -version
exit /b 0
