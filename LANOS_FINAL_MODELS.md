# Final corrected Lanos models integration

- Replaces the previous experimental/scaled Lanos render assets.
- Keeps registry IDs: `domesurvival:lanos_decorative` and `domesurvival:lanos_abandoned`.
- Uses the newly supplied corrected 7 x 3 x 2.5 block models.
- Vehicle bottom remains at Y=0; no additional vertical offset or scaling is applied.
- Models are converted losslessly at cuboid/UV level to Forge OBJ because the final geometry spans multiple blocks.
- Selection outline updated to 7 x 3 x 2.5; collision remains disabled.
- Creative tab code is untouched.

Converted geometry:
- `lanos_decorative`: 126 cuboids, 756 faces, OBJ bounds X=-1.000..2.000, Y=0.000..2.500, Z=-3.000..4.000.
- `lanos_abandoned`: 138 cuboids, 828 faces, OBJ bounds X=-1.000..2.000, Y=0.000..2.500, Z=-3.000..4.000.
