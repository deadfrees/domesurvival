DomeSurvival Custom Paintings V3.2 — compile fix

The V3.1 build reached the actual painting code and failed on exactly one
compiler error:

PaintingVariantTags.create(ResourceLocation)
-> Forge/Minecraft 1.20.1 mapped API expects create(String).

V3.2 changes:
PaintingVariantTags.create(new ResourceLocation(DomeSurvival.MOD_ID, "memory_paintings"))
to:
PaintingVariantTags.create(DomeSurvival.MOD_ID + ":memory_paintings")

It also removes the now-unused ResourceLocation import.

All other warnings in the uploaded log are pre-existing deprecation warnings
and are not the compilation failure.

Install in C:\domesurvival:
  .\APPLY_CUSTOM_PAINTINGS_V3_2_COMPILEFIX.bat

The patch immediately runs compileJava + processResources and writes:
  CUSTOM_PAINTINGS_V3_2_BUILD_LAST.txt
