[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$candidates = [System.Collections.Generic.List[string]]::new()

function Add-JavaCandidate {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return
    }

    try {
        $resolved = (Resolve-Path -LiteralPath $Path -ErrorAction Stop).Path
    } catch {
        return
    }

    if (-not $candidates.Contains($resolved)) {
        $candidates.Add($resolved)
    }
}

Add-JavaCandidate $env:JAVA_HOME

foreach ($vendorRoot in @(
    (Join-Path $env:ProgramFiles "Java"),
    (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
    (Join-Path $env:ProgramFiles "Microsoft")
)) {
    if (Test-Path -LiteralPath $vendorRoot -PathType Container) {
        Get-ChildItem -LiteralPath $vendorRoot -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -like "jdk-17*" } |
            Sort-Object Name -Descending |
            ForEach-Object { Add-JavaCandidate $_.FullName }
    }
}

Get-Command javac.exe -All -ErrorAction SilentlyContinue | ForEach-Object {
    $binDirectory = Split-Path -Parent $_.Source
    Add-JavaCandidate (Split-Path -Parent $binDirectory)
}

foreach ($candidate in $candidates) {
    $javac = Join-Path $candidate "bin\javac.exe"
    $java = Join-Path $candidate "bin\java.exe"
    if (-not (Test-Path -LiteralPath $javac -PathType Leaf) -or
        -not (Test-Path -LiteralPath $java -PathType Leaf)) {
        continue
    }

    $version = (& $javac -version 2>&1 | Out-String).Trim()
    if ($version -match '^javac 17(?:\.|$)') {
        Write-Output $candidate
        exit 0
    }
}

Write-Error "Java 17 JDK was not found. Install a JDK 17 distribution or set JAVA_HOME to it."
exit 1
