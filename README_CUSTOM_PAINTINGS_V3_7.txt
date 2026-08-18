DomeSurvival Custom Paintings V3.7 — Native Registry Fix
===========================================================

Root cause proven by latest.log:
TagLoader reports that every domesurvival painting ID referenced by
#domesurvival:memory_paintings is missing from the painting_variant registry.

Why:
Forge 47.x / Minecraft 1.20.1 exposes PaintingVariant as a normal Forge
registry (ForgeRegistries.PAINTING_VARIANTS). The JSON painting_variant
datapack registration approach used by the previous patches is for newer
Minecraft versions and does not create registry entries on 1.20.1.

V3.7:
- registers all 22 original paintings in Java with DeferredRegister;
- registers all 22 compact fallbacks in Java;
- wires ModPaintingVariants.PAINTING_VARIANTS into the mod event bus;
- keeps textures under assets/domesurvival/textures/painting;
- updates #domesurvival:memory_paintings to all 44 native IDs;
- does NOT add any memory painting to #minecraft:placeable;
- removes obsolete data/domesurvival/painting_variant JSON files from the
  active source tree after backing them up;
- runs a full clean build.

This patch does not touch the world, dome, workshop or Joseph progression.

Install:
  .\APPLY_CUSTOM_PAINTINGS_V3_7_NATIVE_REGISTRY_FIX.bat

Then:
  .\dev\RUN_DEV_FULL.bat

Test:
  /give @s domesurvival:memory_painting
