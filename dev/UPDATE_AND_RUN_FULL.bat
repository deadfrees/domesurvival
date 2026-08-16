@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo ============================================================
echo Dome Survival - UPDATE AND RUN FULL DEV V6.3
echo ============================================================
echo.

call "%~dp0RESTORE_ALL_MODS.bat"
if errorlevel 1 exit /b 1

where git >nul 2>&1
if errorlevel 1 (
    echo [ERROR] git.exe was not found in PATH.
    exit /b 1
)

git rev-parse --is-inside-work-tree >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Current project is not a Git working tree.
    exit /b 1
)

git diff --quiet
if errorlevel 1 (
    echo [STOP] Working tree has uncommitted tracked changes.
    echo Commit or stash them before updating.
    exit /b 1
)

git diff --cached --quiet
if errorlevel 1 (
    echo [STOP] Index has staged but uncommitted changes.
    echo Commit or stash them before updating.
    exit /b 1
)

echo [1/3] git pull --ff-only
git pull --ff-only
if errorlevel 1 (
    echo [ERROR] Git update failed or requires a merge.
    exit /b 1
)

echo.
echo [2/3] Effective full modpack sync
call "%~dp0BOOTSTRAP_FULL_MODPACK.bat"
if errorlevel 1 exit /b 1

echo.
echo [3/3] FULL DEV
call "%~dp0RUN_DEV_FULL.bat"
exit /b %ERRORLEVEL%
