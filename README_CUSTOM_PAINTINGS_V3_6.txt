DomeSurvival Custom Paintings V3.6 — Compact Placement
=========================================================

The first custom collection only contained large artworks:
3x4, 4x3, 4x4 and 4x5 blocks. Vanilla painting placement silently fails when
no registered painting variant fits the available wall. On normal 3-block-high
interior walls this makes most/all of the collection impossible to place.

V3.6 keeps every existing large painting and adds a compact fallback made from
the same already-prepared images:
- landscape -> 3x2 blocks;
- portrait -> 2x3 blocks;
- square -> 2x2 blocks.

No new artwork is generated. Compact textures are resized/cropped copies of the
existing prepared images.

Placement policy:
1. Try the original large 22 paintings first.
2. Search nearby valid wall anchors.
3. If no large painting fits, try the 22 compact variants.
4. Use the vanilla Painting entity and vanilla survives() support check.

Install:
  .\APPLY_CUSTOM_PAINTINGS_V3_6_COMPACT_PLACEMENT.bat

Then:
  .\dev\RUN_DEV_FULL.bat

Test:
  /give @s domesurvival:memory_painting

For the first test use a flat solid wall at least 2 blocks wide x 3 blocks high.
