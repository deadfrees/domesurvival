DomeSurvival Custom Paintings V3.9.1 — Mixed Sizes Hotfix
============================================================

Fixes the V3.9 installer failure:
    V3.9 validation failed for 1x1 painting: 07_pink_hat_portrait

Cause:
V3.9 validation depended on exact Java formatting after the replacement.
V3.9.1 uses regex validation and works with both:
- clean V3.8 source;
- source partially modified by the failed V3.9 installer.

Requested restorations:
- compact_03_airsoft_team -> V3.8 size 3x2 blocks (48x32 px)
- 06_relaxing_on_grass -> V3.8 size 3x4 blocks (48x64 px)

V3.9.1 size layout:
1x1 block (7):
- 07 pink hat portrait
- 09 white hat portrait
- 10 flexing portrait
- 12 kitchen character
- 16 tricolor portrait
- 17 bee hero amber hive
- 19 night selfie friendship

2x2 blocks / 4 blocks total (6):
- 01 trio friends
- 03 airsoft team
- 13 music studio friends
- 15 voxel company bright light
- 18 wedding kiss tree
- 22 party toast indoor

Restored / kept large:
- 06 relaxing on grass -> 3x4
- all other paintings not listed above retain their current V3.8 dimensions
- compact_03_airsoft_team -> 3x2

No world, quest, dome or workshop data is modified.

Install:
  .\APPLY_CUSTOM_PAINTINGS_V3_9_1_MIXED_SIZES_HOTFIX.bat

Then:
  .\dev\RUN_DEV_FULL.bat
