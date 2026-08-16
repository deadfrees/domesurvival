@echo off
setlocal EnableExtensions
cd /d "%~dp0.."

echo ============================================================
echo Dome Survival - UPDATE AND RUN FULL DEV V6.8 STABLE
echo ============================================================
echo.

call "%~dp0RESTORE_ALL_MODS.bat"
if errorlevel 1 exit /b 1

where git.exe >nul 2>&1
if errorlevel 1 (
    echo [ERROR] git.exe was not found in PATH.
    exit /b 1
)

git rev-parse --is-inside-work-tree >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Current directory is not a Git working tree.
    exit /b 1
)

git diff --quiet
if errorlevel 1 (
    echo [STOP] Tracked working-tree changes exist.
    echo Commit or stash them before updating.
    exit /b 1
)

git diff --cached --quiet
if errorlevel 1 (
    echo [STOP] Staged but uncommitted changes exist.
    echo Commit or stash them before updating.
    exit /b 1
)

echo [1/2] git pull --ff-only
git pull --ff-only
if errorlevel 1 (
    echo [ERROR] Git update failed or requires a merge.
    exit /b 1
)

echo.
echo [2/2] FULL DEV
call "%~dp0RUN_DEV_FULL.bat"
exit /b %ERRORLEVEL%
