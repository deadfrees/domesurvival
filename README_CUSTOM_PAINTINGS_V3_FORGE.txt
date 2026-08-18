DomeSurvival Custom Paintings V3.1 — FORGE NATIVE
================================================

This replaces the old KubeJS V1/V2 implementation.

Why V1/V2 could appear to add nothing:
- the old item was registered through a KubeJS startup script rather than the
  actual DomeSurvival Forge item registry;
- the old KubeJS registration used event.create("memory_painting") while the
  recipe/placement code expected domesurvival:memory_painting;
- the old painting_variant JSONs used texture pixel sizes (64x48, 64x80, ...)
  instead of painting dimensions in blocks (4x3, 4x5, ...).

V3:
- registers domesurvival:memory_painting in ModItems;
- adds MemoryPaintingItem.java;
- uses vanilla minecraft:painting entities;
- selects ONLY #domesurvival:memory_paintings variants;
- tries random variants until one fits the clicked wall;
- does not modify the vanilla painting pool;
- includes all 22 prepared images;
- uses corrected painting dimensions in blocks;
- recipe is one vanilla painting -> one Memory Painting;
- puts the item in the existing Dome Survival creative tab;
- moves stale V1/V2 KubeJS painting files to the patch backup after a successful build.

Install:
  Expand the archive into C:\domesurvival
  .\APPLY_CUSTOM_PAINTINGS_V3_FORGE.bat

The installer runs:
  gradlew.bat compileJava processResources --no-daemon

If verification succeeds:
  .\dev\RUN_DEV_FULL.bat

Test:
  /give @s domesurvival:memory_painting

Then place it on a sufficiently large vertical wall.

V3.1 installer fix
------------------
PowerShell 5.1 can report ordinary javac/Gradle STDERR warnings as
NativeCommandError when ErrorActionPreference is Stop. V3.1 runs the build
through cmd.exe, captures stdout+stderr into CUSTOM_PAINTINGS_V3_BUILD_LAST.txt,
and decides success only from Gradle's actual exit code.

Re-running this patch over a partially applied V3 install is safe: the Java
registration check is idempotent and the resource payload is simply refreshed.
