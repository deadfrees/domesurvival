DomeSurvival Custom Paintings V3.4 — Placement Fix

Symptom:
- domesurvival:memory_painting exists, but right-clicking a wall does not place it.

V3.4 changes:
- no longer depends on #domesurvival:memory_paintings being available at runtime;
- resolves the 22 known painting variants directly from Registries.PAINTING_VARIANT;
- keeps vanilla minecraft:painting entities for rendering/breaking/support;
- does NOT add custom paintings to minecraft:placeable, so ordinary vanilla
  paintings stay separate;
- searches nearby anchor positions on the same wall because all custom artworks
  are large (minimum roughly 3x4 blocks), making placement much less finicky;
- runs compileJava + processResources automatically.

Install:
  .\APPLY_CUSTOM_PAINTINGS_V3_4_PLACEMENT_FIX.bat

Then:
  .\dev\RUN_DEV_FULL.bat

Test:
  /give @s domesurvival:memory_painting

Use a flat solid wall at least 4 blocks wide and 5 blocks high for the first test.
