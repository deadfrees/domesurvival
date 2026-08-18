DomeSurvival Custom Paintings V3.9.2 — Validator Fix
======================================================

This is a direct hotfix for the uploaded V3.9.1 archive.

Observed failure:
  V3.9.1 size validation failed for 06_relaxing_on_grass:
  expected 48x64px

V3.9.1 wrote ModPaintingVariants.java BEFORE running its final regex
validation. Therefore a failed V3.9.1 may already be partially applied.

V3.9.2 is intentionally idempotent and safe in that state.

Changes:
- removes the fragile regex-based size rewrite/validation;
- finds each registry entry by its exact painting ID;
- parses the two PaintingVariant integer arguments numerically;
- modifies only those two integer arguments;
- validates the full layout in memory BEFORE writing ModPaintingVariants.java;
- validates the file again AFTER writing;
- reapplies the exact texture payload from the supplied V3.9.1 archive.

Required restored sizes:
- 06_relaxing_on_grass: 48x64 px = 3x4 blocks
- compact_03_airsoft_team: 48x32 px = 3x2 blocks

Install:
  .\APPLY_CUSTOM_PAINTINGS_V3_9_2_VALIDATOR_FIX.bat

Then:
  .\dev\RUN_DEV_FULL.bat
