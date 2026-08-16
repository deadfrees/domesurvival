$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$Path = Join-Path $ProjectRoot '.gitignore'
$Marker = '# DomeSurvival generated FULL DEV runtime cache'

if (-not (Test-Path -LiteralPath $Path)) {
    [System.IO.File]::WriteAllText($Path, "$Marker`r`ndev/generated/`r`n")
    exit 0
}

$text = [System.IO.File]::ReadAllText($Path)
if ($text -notmatch [regex]::Escape($Marker)) {
    if (-not $text.EndsWith("`n")) { $text += "`r`n" }
    $text += "`r`n$Marker`r`ndev/generated/`r`n"
    [System.IO.File]::WriteAllText($Path, $text, (New-Object System.Text.UTF8Encoding($false)))
}
