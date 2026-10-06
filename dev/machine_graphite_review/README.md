# Graphite machine finish — 2026-09-29

The shared procedural satin atlas now includes stepped graphite tones, fine grain,
an inset panel edge and restrained edge wear. Its UV layout is unchanged and is
checked against the current generator export during rebaking. Only the generator,
press, purifier and their connectors reference this atlas.

The purifier has raised service covers, handles, captive bolts, reinforcing ribs
and a rear cooling grille. The central port area stays clear. The energy number
has been removed from the main GUI; the gauge tooltip retains stored/capacity FE.

Reproduce with Blender, in this order:

1. `source_assets/blender/scripts/refresh_machine_graphite.py`
2. `source_assets/blender/scripts/create_forming_press_v2.py`
3. `source_assets/blender/scripts/create_water_purifier_v2.py`
4. `source_assets/blender/scripts/review_machine_graphite.py`

`machine_materials.png` compares the actual exported static meshes in the same
lighting. `purifier_service_sides.png` shows the new rear and side hardware.
Fluid contents and animated parts are driven by the in-game renderer.

Validation: Gradle `test build --offline` succeeded; the geometry audit passed all
40 rotated purifier port configurations. In-game GUI screenshots and the runtime
report are in `dev/water_purifier_v2/runtime`.
