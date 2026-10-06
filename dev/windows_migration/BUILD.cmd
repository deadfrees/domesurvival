@echo off
setlocal
set "JAVA_HOME=%~dp0toolchain\jdk-17.0.12"
set "GRADLE_USER_HOME=%~dp0toolchain\gradle-home"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0project"
call gradlew.bat test build --offline --console=plain
pause
