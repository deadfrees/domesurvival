# DomeSurvival V3.2.6.1 — M40 alignment hotfix

## Problem
After V3.2.6, the M40 mask geometry was visibly sunk into the player's head.
The front filter was partially visible, while other parts of the mask clipped through
the head volume and became visible on the back of the head.

## Root cause
The runtime GLB conversion and renderer were functioning, but the final head-space
translation was too deep on the Z axis, leaving too much of the mask inside the head mesh.

## Fix
The exact same converted M40 geometry is kept.
Only the final placement transform was adjusted:

- scale: 0.72
- Y offset: -0.265
- Z offset: -0.255

Compared to V3.2.6, the mask is now:
- shifted forward significantly;
- shifted slightly upward.

## Result
This patch replaces only:

`src/main/resources/assets/domesurvival/models/m40_mask_mesh.bin`

No gameplay code changes.
No oxygen logic changes.
No tank model changes.
No structure/network changes.

## New fitted bounds
min = [-0.22050000727176666, -0.4765000343322754, -0.5283750295639038]
max = [0.22050000727176666, -0.053499966859817505, 0.018375001847743988]
triangles = 300
vertices = 600
