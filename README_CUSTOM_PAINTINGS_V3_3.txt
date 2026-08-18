DomeSurvival Custom Paintings V3.3 — TagKey compile fix

The V3.2 log proves the previous call reached a private helper:
  PaintingVariantTags.create(String) has private access

V3.3 uses the public vanilla/Forge-supported TagKey factory instead:
  TagKey.create(
      Registries.PAINTING_VARIANT,
      new ResourceLocation(DomeSurvival.MOD_ID, "memory_paintings")
  )

This matches Forge 1.20.1 tag guidance for vanilla registries.

Install in C:\domesurvival:
  .\APPLY_CUSTOM_PAINTINGS_V3_3_TAGKEY_FIX.bat

The patch immediately runs:
  gradlew.bat compileJava processResources --no-daemon

Build log:
  CUSTOM_PAINTINGS_V3_3_BUILD_LAST.txt
