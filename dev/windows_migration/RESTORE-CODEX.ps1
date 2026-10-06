$ErrorActionPreference = 'Stop'
if (Get-Process -Name Codex,ChatGPT -ErrorAction SilentlyContinue) {
    throw 'Close Codex/ChatGPT before restoring its local data.'
}
$python = Join-Path $PSScriptRoot 'project\dev\fluid_pipes\runtime_blender\5.2\python\bin\python.exe'
$script = Join-Path $PSScriptRoot 'project\dev\windows_migration\restore_codex.py'
& $python -X utf8 $script $PSScriptRoot
if ($LASTEXITCODE -ne 0) { throw 'Codex restore did not complete.' }
