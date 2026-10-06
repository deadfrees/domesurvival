@echo off
setlocal
"%~dp0project\dev\fluid_pipes\runtime_blender\5.2\python\bin\python.exe" -X utf8 "%~dp0project\dev\windows_migration\backup.py" "%~dp0." --verify
pause
