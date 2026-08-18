DomeSurvival Custom Paintings V3.9.4 — Full Registry Overwrite
================================================================

This patch fixes the repeated V3.9.x installer failure by removing the fragile
Java text-editing stage completely.

V3.9.4 does NOT regex-patch ModPaintingVariants.java.
It backs up the current file and replaces the entire registry source with one
known-good authoritative Forge 47.4.x / Minecraft 1.20.1 file.

Final layout:
1x1 block:
  07, 09, 10, 12, 16, 17, 19

2x2 blocks:
  01, 03, 13, 15, 18, 22

Large/original:
  02 = 4x5
  04 = 4x5
  05 = 3x4
  06 = 3x4
  08 = 4x5
  11 = 4x5
  14 = 4x5
  20 = 4x5
  21 = 4x5

All 22 compact fallback variants remain at V3.8 compact sizes.
In particular:
  compact_03_airsoft_team = 3x2

The exact texture corrections from the user's supplied V3.9.1 hotfix are
reapplied.

The installer verifies the Java registry copy byte-for-byte with SHA-256,
checks all 44 registrations, then runs the full clean build.

Install:
  .\APPLY_CUSTOM_PAINTINGS_V3_9_4_FULL_REGISTRY_OVERWRITE.bat

Then:
  .\dev\RUN_DEV_FULL.bat
