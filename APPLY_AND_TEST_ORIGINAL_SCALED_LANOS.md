# DomeSurvival — original Lanos models, uniformly scaled

This patch restores the first working models from `lanos.zip` and `lanosold.zip`.

## What changed

- `lanos_decorative` and `lanos_abandoned` again use the original working JSON geometry and original textures/UV.
- Geometry is scaled with ONE factor on X, Y and Z: `2.445414847...`.
- Target length is exactly 7 blocks.
- Because proportions are preserved, final approximate dimensions are:
  - length: 7.00 blocks;
  - width: 3.76 blocks;
  - height: 2.42 blocks (`lanos_decorative`) / 2.45 blocks (`lanos_abandoned`).
- The model is recentered around the block anchor horizontally.
- Original minimum model Y was `2.5`; after scaling it is explicitly shifted to `Y=0`. The vehicle therefore rests on the placement surface instead of floating.
- Item display scales are compensated so the large world model still fits reasonably in inventory/hand views.
- Registry IDs are unchanged, so existing placed blocks/items remain the same IDs.
- The `Dome Survival` creative tab is not replaced by this patch and remains active from the previous update.

## Clean up the failed OBJ experiment

After extracting this patch, remove the now-unused OBJ/MTL files:

```powershell
Remove-Item .\src\main\resources\assets\domesurvival\models\block\lanos_decorative.obj -Force -ErrorAction SilentlyContinue
Remove-Item .\src\main\resources\assets\domesurvival\models\block\lanos_decorative.mtl -Force -ErrorAction SilentlyContinue
Remove-Item .\src\main\resources\assets\domesurvival\models\block\lanos_abandoned.obj -Force -ErrorAction SilentlyContinue
Remove-Item .\src\main\resources\assets\domesurvival\models\block\lanos_abandoned.mtl -Force -ErrorAction SilentlyContinue
```

## Build

```powershell
.\gradlew.bat clean build
```

## Test

```mcfunction
/give @s domesurvival:lanos_decorative
/give @s domesurvival:lanos_abandoned
```

Place both models on a flat surface and check all four directions. The lowest visible geometry should touch the surface with no air gap.
