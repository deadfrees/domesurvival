$ErrorActionPreference = "Stop"

$Root = (Resolve-Path $PSScriptRoot).Path
$Joseph = Join-Path $Root "CUSTOMNPCS_STAGE4\joseph_cooper_gui.js"
$CommandJava = Join-Path $Root "src\main\java\com\wasted\domesurvival\forge\integration\customnpcs\JosephScriptCommand.java"

if (-not (Test-Path -LiteralPath $Joseph)) {
    throw "Joseph GUI script not found: $Joseph"
}
if (-not (Test-Path -LiteralPath $CommandJava)) {
    throw "JosephScriptCommand.java not found: $CommandJava"
}

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$Js = [IO.File]::ReadAllText($Joseph, [Text.Encoding]::UTF8)
$Java = [IO.File]::ReadAllText($CommandJava, [Text.Encoding]::UTF8)

$Stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$Backup = Join-Path $Root "_patch_backups\joseph_v741_interact_fix_$Stamp"
New-Item -ItemType Directory -Force -Path $Backup | Out-Null
Copy-Item -LiteralPath $Joseph -Destination (Join-Path $Backup "joseph_cooper_gui.js") -Force
Copy-Item -LiteralPath $CommandJava -Destination (Join-Path $Backup "JosephScriptCommand.java") -Force

Write-Host "============================================================"
Write-Host "DomeSurvival Joseph V7.4.1 - INTERACT / HELLO DEV FIX"
Write-Host "============================================================"
Write-Host ""

# ---------------------------------------------------------------------------
# 1) Make the JS interaction event explicitly cancel CustomNPCs' vanilla
#    fallback interaction path. Without cancellation CustomNPCs may continue
#    to Advanced -> Interact Lines and say the old "Hello Dev" line.
# ---------------------------------------------------------------------------
if (-not $Js.Contains("DOMESURVIVAL_V741_CANCEL_INTERACT")) {
    $rx = [regex]::new(
        'function\s+interact\s*\(\s*e\s*\)\s*\{',
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )
    $matches = $rx.Matches($Js)
    if ($matches.Count -ne 1) {
        throw "Expected exactly one function interact(e) in joseph_cooper_gui.js, found $($matches.Count)."
    }

    $replacement = @'
function interact(e) {
    /* DOMESURVIVAL_V741_CANCEL_INTERACT
       Critical: prevent CustomNPCs from falling through to Advanced -> Interact Lines. */
    try { e.setCanceled(true); } catch (ignoredCancel) {}
'@

    $Js = $rx.Replace(
        $Js,
        [System.Text.RegularExpressions.MatchEvaluator]{
            param($m)
            return $replacement
        },
        1
    )
}

# ---------------------------------------------------------------------------
# 2) /josephscript apply now also clears old Advanced -> Interact Lines.
#    Reflection is used deliberately so we do not bind this integration patch
#    to one unofficial CustomNPCs internal class name.
# ---------------------------------------------------------------------------
if (-not $Java.Contains("clearLegacyInteractSpeech(npc);")) {
    $anchor = "        npc.updateClient();"
    if (-not $Java.Contains($anchor)) {
        throw "Could not find npc.updateClient() in JosephScriptCommand.apply()."
    }
    $Java = $Java.Replace(
        $anchor,
        "        clearLegacyInteractSpeech(npc);`r`n`r`n" + $anchor
    )
}

if (-not $Java.Contains("private static void clearLegacyInteractSpeech(")) {
    $helper = @'

    /**
     * Clears old CustomNPCs Advanced -> Interact Lines left from early
     * development (for example "Hello Dev").
     *
     * This is intentionally reflection-based because CustomNPCs unofficial
     * ports have changed internal data classes between builds.
     */
    private static void clearLegacyInteractSpeech(EntityNPCInterface npc) {
        if (npc == null) {
            return;
        }

        try {
            Object advanced = readFieldRecursive(npc, "advanced");
            if (advanced == null) {
                return;
            }

            Object interactLines = readFieldRecursive(advanced, "interactLines");
            if (interactLines == null) {
                return;
            }

            Object lines = readFieldRecursive(interactLines, "lines");
            if (lines instanceof Map<?, ?> map) {
                map.clear();
            }
        } catch (Throwable error) {
            System.err.println("[DomeSurvival] Could not clear legacy Joseph interact lines:");
            error.printStackTrace();
        }
    }

    private static Object readFieldRecursive(Object owner, String fieldName) throws ReflectiveOperationException {
        Class<?> type = owner.getClass();

        while (type != null) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }

        throw new NoSuchFieldException(fieldName);
    }

'@

    $anchor = "    private static EntityNPCInterface findJoseph(ServerPlayer player) {"
    if (-not $Java.Contains($anchor)) {
        throw "Could not find findJoseph() anchor in JosephScriptCommand.java."
    }
    $Java = $Java.Replace($anchor, $helper + $anchor)
}

# Improve success output so it is obvious the legacy speech cleanup ran.
$oldSuccess = @'
            "[JosephScript] Old inline scripts removed. "
                + SCRIPT_FILE + " linked. language=" + finalLanguage
'@
$newSuccess = @'
            "[JosephScript] Old inline scripts removed; legacy interact speech cleared. "
                + SCRIPT_FILE + " linked. language=" + finalLanguage
'@
if ($Java.Contains($oldSuccess)) {
    $Java = $Java.Replace($oldSuccess, $newSuccess)
}

# Version marker where possible; do not fail if the source has a slightly
# different V7.x marker.
if ($Js.Contains("/* Dome Survival - Joseph Cooper GUI v7.4 EXODUS */")) {
    $Js = $Js.Replace(
        "/* Dome Survival - Joseph Cooper GUI v7.4 EXODUS */",
        "/* Dome Survival - Joseph Cooper GUI v7.4.1 INTERACT FIX */"
    )
}

# Validation before write.
$RequiredJs = @(
    "function interact(e)",
    "DOMESURVIVAL_V741_CANCEL_INTERACT",
    "e.setCanceled(true)"
)
foreach ($marker in $RequiredJs) {
    if (-not $Js.Contains($marker)) {
        throw "JS validation failed. Missing: $marker"
    }
}

$RequiredJava = @(
    "clearLegacyInteractSpeech(npc);",
    "private static void clearLegacyInteractSpeech(",
    'readFieldRecursive(advanced, "interactLines")',
    'readFieldRecursive(interactLines, "lines")',
    "map.clear();"
)
foreach ($marker in $RequiredJava) {
    if (-not $Java.Contains($marker)) {
        throw "Java validation failed. Missing: $marker"
    }
}

[IO.File]::WriteAllText($Joseph, $Js, $Utf8NoBom)
[IO.File]::WriteAllText($CommandJava, $Java, $Utf8NoBom)

# Refresh the external CustomNPCs script copies.
$Targets = New-Object System.Collections.Generic.List[string]
$Targets.Add((Join-Path $Root "run\customnpcs\scripts\ecmascript"))

$WorldLocal = Join-Path $Root "run\saves\WASTED_TEST\customnpcs\scripts\ecmascript"
if (Test-Path -LiteralPath $WorldLocal) {
    $Targets.Add($WorldLocal)
}

foreach ($TargetDir in $Targets) {
    New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null
    $Target = Join-Path $TargetDir "joseph_cooper_gui.js"
    Copy-Item -LiteralPath $Joseph -Destination $Target -Force

    $A = (Get-FileHash -LiteralPath $Joseph -Algorithm SHA256).Hash
    $B = (Get-FileHash -LiteralPath $Target -Algorithm SHA256).Hash
    if ($A -ne $B) {
        throw "Script copy verification failed: $Target"
    }

    Write-Host "[OK] Joseph script -> $Target" -ForegroundColor Green
}

Write-Host ""
Write-Host "[OK] Joseph V7.4.1 interaction fix applied." -ForegroundColor Green
Write-Host "Backup: $Backup"
Write-Host ""
Write-Host "Next:"
Write-Host "  1. .\dev\RUN_DEV_FULL.bat"
Write-Host "  2. In world: /josephscript apply"
Write-Host "  3. Then: /josephscript inspect"
Write-Host "  4. Right-click Joseph"
Write-Host ""
Write-Host "Expected: quest GUI opens and 'Hello Dev' no longer appears."
exit 0
