DomeSurvival Custom Paintings V3.9.3 — Direct Size Fix
=========================================================

Observed on the user's current source after failed V3.9.1/V3.9.2:
  06_relaxing_on_grass = 32x32 px
but it must be restored to:
  48x64 px = 3x4 blocks

Root installer issue:
V3.9.1 and V3.9.2 restored special sizes through a hashtable/enumerator
argument path. On the user's PowerShell execution that restore did not take
effect, even though the later validator correctly detected the wrong 32x32
state.

V3.9.3 removes that path completely.

The special restores are now literal named calls and are deliberately applied
LAST:
  06_relaxing_on_grass      -> 48x64
  compact_03_airsoft_team   -> 48x32

The installer verifies each write immediately, then validates again before
writing the Java file, and validates a third time after the file is written.

It is safe over partially applied V3.9 / V3.9.1 / V3.9.2.

The texture payload is copied directly from the user's supplied V3.9.1 hotfix.
The restored textures are:
  06_relaxing_on_grass.png        192x256
  compact_03_airsoft_team.png     192x128

Install:
  .\APPLY_CUSTOM_PAINTINGS_V3_9_3_DIRECT_SIZE_FIX.bat

Then:
  .\dev\RUN_DEV_FULL.bat
