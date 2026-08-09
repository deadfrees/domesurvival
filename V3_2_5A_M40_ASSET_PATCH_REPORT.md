# DomeSurvival V3.2.5a — M40 asset patch

## Goal
Add the recolored M40 gas mask source asset to the project as a patch-ready repository addition.

## Included files
The patch adds:

`dev/reference_assets/oxygen_mask_m40_domesurvival/`
- `m40_gasmask_domesurvival_recolored.glb`
- `m40_gasmask_domesurvival_recolored_texture.png`
- `m40_gasmask_domesurvival_recolored_texture_preview.png`
- `README.md`

## What this patch changes
- Adds the recolored gas mask model and texture to the repository.
- Does **not** change gameplay code.
- Does **not** change oxygen mechanics.
- Does **not** replace the current Java runtime mask renderer automatically.

## Why
The user explicitly requested taking the provided mask model and **simply recoloring it** without inventing a new design.
This patch stores that approved recolored asset in the project tree so both developers can keep the exact source file in git.

## Technical note
The current DomeSurvival runtime equipment path is based on Java HumanoidModel rendering.
Direct use of `.glb` at runtime would require an additional renderer / conversion path and is intentionally kept out of this patch.

## Safe to apply
Yes.
This patch is additive and low-risk:
- no structure changes;
- no network changes;
- no gameplay changes.
