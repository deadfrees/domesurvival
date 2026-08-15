# M40 gas mask — DomeSurvival recolored asset

This folder contains the **recolored source asset** requested by the project.

## Files
- `m40_gasmask_domesurvival_recolored.glb`
- `m40_gasmask_domesurvival_recolored_texture.png`
- `m40_gasmask_domesurvival_recolored_texture_preview.png`

## What this patch does
This patch adds the recolored M40 gas mask asset to the `domesurvival` repository as a tracked development/reference asset.

## Important
This is an **asset patch**, not an automatic runtime renderer integration.

Minecraft Forge / the current DomeSurvival equipment renderer does **not** consume `.glb` files directly.
The current in-game mask still uses the Java-side custom armor model pipeline.

So this patch is intended for:
- source control;
- handoff to the first/second developer;
- reference for the next runtime integration step;
- future conversion into the final Minecraft model/render implementation.

## Palette
The model was recolored into the established DomeSurvival palette:
- dark graphite;
- neutral grey metal;
- cyan / light-cyan accents.
