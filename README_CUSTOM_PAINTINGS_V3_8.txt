DomeSurvival Custom Paintings V3.8 — Render + Drop Fix
======================================================

Two separate bugs are fixed.

1) Empty painting front
-----------------------
Forge/Minecraft 1.20.1 PaintingVariant(width, height) stores dimensions in
PIXELS. V3.7 registered block counts such as 4x3, 4x5, 2x3.

Minecraft's vanilla painting renderer builds the mesh in 16x16-pixel tiles.
A width/height smaller than 16 produces no render tiles, so the entity exists
but the painting front appears empty.

V3.8 registers:
  4x3 blocks -> 64x48 pixels
  4x5 blocks -> 64x80 pixels
  3x4 blocks -> 48x64 pixels
  4x4 blocks -> 64x64 pixels
  compact 3x2 -> 48x32 pixels
  compact 2x3 -> 32x48 pixels
  compact 2x2 -> 32x32 pixels

2) Vanilla painting item drops
------------------------------
A vanilla Painting entity always drops minecraft:painting. V3.8 introduces a
persistent MemoryPaintingEntity subclass with its own EntityType and delegates
rendering to Minecraft's PaintingRenderer.

Its dropItem implementation drops:
  domesurvival:memory_painting

This applies when the painting itself is broken and when its supporting wall
block disappears. Because it has its own EntityType, the custom drop behavior
survives world save/reload.

The patch also removes the unnecessary manual
assets/minecraft/atlases/paintings.json override if present (backup retained).
Minecraft 1.20.1's vanilla paintings atlas already scans textures/painting.

Install:
  .\APPLY_CUSTOM_PAINTINGS_V3_8_RENDER_DROP_FIX.bat

Then:
  .\dev\RUN_DEV_FULL.bat

Test:
  /give @s domesurvival:memory_painting
