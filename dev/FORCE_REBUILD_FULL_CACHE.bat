@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo ============================================================
echo Dome Survival - FORCE FULL DEV V6.8 REBUILD
echo ============================================================
echo.

if exist "dev\generated\full_modpack.generator_version.txt" del /Q "dev\generated\full_modpack.generator_version.txt"
if exist "dev\generated\full_modpack_runtime.gradle" del /Q "dev\generated\full_modpack_runtime.gradle"
if exist "dev\generated\full_modpack.fingerprint.txt" del /Q "dev\generated\full_modpack.fingerprint.txt"
if exist "dev\generated\mixin_srg_bridge_report.txt" del /Q "dev\generated\mixin_srg_bridge_report.txt"

call "%~dp0PREPARE_FULL_DEV_RUNTIME.bat"
exit /b %ERRORLEVEL%
