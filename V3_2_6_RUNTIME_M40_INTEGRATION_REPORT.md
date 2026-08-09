# DomeSurvival V3.2.6 — Runtime M40 mask integration

## What changed

The oxygen mask now uses the geometry from the user-provided `m40_gasmask.glb`.
It is no longer a hand-built Minecraft cuboid interpretation.

Source mesh after GLB transforms:
- vertices: 600
- triangles: 300
- mesh parts: 25
- original dimensions: 0.612500 x 0.587500 x 0.759375

## Conversion

Development-time conversion only:
1. Read all GLB nodes and node transforms.
2. Read POSITION, NORMAL, TEXCOORD_0 and triangle indices.
3. Preserve the original triangle geometry and UV coordinates.
4. Uniformly scale by `0.72` to fit the Minecraft player head.
5. Convert glTF Y-up coordinates to the Minecraft model coordinate orientation.
6. Store the resulting vertex/index data in `assets/domesurvival/models/m40_mask_mesh.bin`.

No approximation with Minecraft cubes is used for the visible mask.

## Runtime

`M40MaskRenderLayer` is added to both vanilla player skin renderers (`default` and `slim`).
It renders only when:

`HEAD == domesurvival:oxygen_mask`

The render layer follows `PlayerModel.head.translateAndRotate(...)`, so the mask follows
player head yaw/pitch and normal player animation.

The old `OxygenMaskModel` is intentionally empty, preventing a second mask from being
rendered by the vanilla armor layer.

## Texture

Runtime texture:
`assets/domesurvival/textures/models/armor/m40_gasmask_domesurvival.png`

This is the recolored texture made directly from the embedded texture in the supplied GLB.

Palette:
- graphite
- grey metal
- cyan
- light cyan

## Oxygen tank

No tank model code is changed by this patch.

## Gameplay / server

No oxygen gameplay logic is changed.
No server tick logic is changed.
No packets are changed.

This is client rendering only.

## Performance

The source mask contains only 300 triangles.
It is rendered only for visible players currently wearing the oxygen mask.

There is:
- no GLB parsing every frame;
- no file IO every frame;
- no server cost;
- no block/chunk scan.

The preconverted ~23 KB mesh resource is loaded once on the client and retained in memory; it is then submitted as one textured player render layer.

## Structure / network

- structureVersion remains 7/7
- network protocol remains 2
- no `/dome upgrade`
